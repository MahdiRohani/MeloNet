#!/usr/bin/env python3
"""Voice cover worker: Redis queue → Demucs/RVC (or stub) → MinIO → Postgres."""

from __future__ import annotations

import json
import logging
import os
import signal
import sys
import tempfile
import time
import traceback
from pathlib import Path
from typing import Any

import psycopg
import requests
from minio import Minio
from redis import Redis
from redis.exceptions import TimeoutError as RedisTimeoutError

from pipeline import process_cover

LOG = logging.getLogger("voice-worker")

QUEUE_KEY = "voice_cover:jobs"
MAX_ATTEMPTS = int(os.getenv("VOICE_WORKER_MAX_ATTEMPTS", "3"))
JOB_TIMEOUT_SEC = int(os.getenv("VOICE_WORKER_JOB_TIMEOUT_SEC", "1800"))
BRPOP_TIMEOUT_SEC = int(os.getenv("VOICE_WORKER_BRPOP_TIMEOUT_SEC", "5"))
CONCURRENCY = max(1, int(os.getenv("VOICE_WORKER_CONCURRENCY", "1")))
MODE = os.getenv("VOICE_WORKER_MODE", "rvc").strip().lower()

_running = True


class PermanentJobError(RuntimeError):
    """Non-retryable job failure (e.g. missing RVC model)."""


def env(name: str, default: str = "") -> str:
    return os.getenv(name, default).strip()


def setup_logging() -> None:
    level = env("LOG_LEVEL", "INFO").upper()
    logging.basicConfig(
        level=getattr(logging, level, logging.INFO),
        format='{"time":"%(asctime)s","level":"%(levelname)s","msg":"%(message)s"}',
    )


def connect_redis() -> Redis:
    url = env("REDIS_URL", "redis://localhost:6379/0")
    # socket_timeout must stay unset/None so BRPOP can block for its own timeout
    # without the client raising redis.exceptions.TimeoutError.
    client = Redis.from_url(
        url,
        decode_responses=True,
        socket_connect_timeout=5,
        socket_timeout=None,
        health_check_interval=30,
    )
    client.ping()
    return client


def connect_db() -> psycopg.Connection:
    dsn = env("DATABASE_URL")
    if not dsn:
        raise RuntimeError("DATABASE_URL is required")
    return psycopg.connect(dsn, autocommit=True)


def connect_minio() -> tuple[Minio, str]:
    endpoint = env("STORAGE_ENDPOINT", "localhost:9000")
    access = env("STORAGE_ACCESS_KEY", "melonet")
    secret = env("STORAGE_SECRET_KEY", "melonetsecret")
    bucket = env("STORAGE_BUCKET", "melonet-media")
    use_ssl = env("STORAGE_USE_SSL", "false").lower() in {"1", "true", "yes"}
    client = Minio(endpoint, access_key=access, secret_key=secret, secure=use_ssl)
    if not client.bucket_exists(bucket):
        client.make_bucket(bucket)
    return client, bucket


def fetch_cover(conn: psycopg.Connection, cover_id: int) -> dict[str, Any] | None:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT
                c.id, c.source_song_id, c.status, c.attempt_count,
                c.cover_url, c.source_title, c.source_artist,
                a.slug, a.display_name, a.model_path, a.pitch_default
            FROM voice_covers c
            JOIN voice_artists a ON a.id = c.target_artist_id
            WHERE c.id = %s
            """,
            (cover_id,),
        )
        row = cur.fetchone()
        if not row:
            return None
        keys = [
            "id", "source_song_id", "status", "attempt_count",
            "cover_url", "source_title", "source_artist",
            "artist_slug", "artist_name", "model_path", "pitch_default",
        ]
        return dict(zip(keys, row))


def mark_processing(conn: psycopg.Connection, cover_id: int) -> int | None:
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE voice_covers
            SET status = 'processing',
                attempt_count = attempt_count + 1,
                error = '',
                progress_pct = 1,
                progress_stage = 'queued',
                eta_seconds = 0,
                updated_at = NOW()
            WHERE id = %s
              AND status IN ('pending', 'failed', 'processing')
              AND attempt_count < %s
            RETURNING attempt_count
            """,
            (cover_id, MAX_ATTEMPTS),
        )
        row = cur.fetchone()
        return int(row[0]) if row else None


def update_progress(
    conn: psycopg.Connection,
    cover_id: int,
    pct: int,
    stage: str,
    eta_seconds: int,
) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE voice_covers
            SET progress_pct = %s,
                progress_stage = %s,
                eta_seconds = %s,
                updated_at = NOW()
            WHERE id = %s AND status = 'processing'
            """,
            (max(0, min(100, pct)), (stage or "")[:64], max(0, int(eta_seconds)), cover_id),
        )


def mark_ready(conn: psycopg.Connection, cover_id: int, object_key: str) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE voice_covers
            SET status = 'ready',
                audio_object_key = %s,
                error = '',
                progress_pct = 100,
                progress_stage = 'done',
                eta_seconds = 0,
                updated_at = NOW(),
                ready_at = NOW()
            WHERE id = %s
            """,
            (object_key, cover_id),
        )


def mark_failed(conn: psycopg.Connection, cover_id: int, error: str) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            UPDATE voice_covers
            SET status = 'failed',
                error = %s,
                progress_stage = 'failed',
                eta_seconds = 0,
                updated_at = NOW()
            WHERE id = %s
            """,
            (error[:2000], cover_id),
        )


def download_source(song_id: str, dest: Path) -> None:
    public_base = env("PUBLIC_BASE_URL", "http://localhost:8080").rstrip("/")
    # Prefer our stream proxy so the worker doesn't need Audius discovery.
    url = f"{public_base}/api/stream/{song_id}"
    LOG.info("downloading source song_id=%s url=%s", song_id, url)
    with requests.get(url, stream=True, timeout=120, allow_redirects=True) as resp:
        resp.raise_for_status()
        with dest.open("wb") as f:
            for chunk in resp.iter_content(chunk_size=1024 * 256):
                if chunk:
                    f.write(chunk)
    if dest.stat().st_size <= 0:
        raise RuntimeError("downloaded source audio is empty")


def upload_result(minio_client: Minio, bucket: str, cover_id: int, audio_path: Path) -> str:
    object_key = f"voice-covers/{cover_id}.mp3"
    content_type = "audio/mpeg"
    minio_client.fput_object(bucket, object_key, str(audio_path), content_type=content_type)
    return object_key


def load_artist_pitch(models_dir: Path, slug: str, fallback: int) -> int:
    meta_path = models_dir / slug / "meta.json"
    if not meta_path.exists():
        return fallback
    try:
        data = json.loads(meta_path.read_text(encoding="utf-8"))
        return int(data.get("pitch_default", fallback) or fallback)
    except (OSError, ValueError, TypeError, json.JSONDecodeError):
        return fallback


def handle_job(
    conn: psycopg.Connection,
    minio_client: Minio,
    bucket: str,
    cover_id: int,
    models_dir: Path,
    work_dir: Path,
) -> None:
    cover = fetch_cover(conn, cover_id)
    if cover is None:
        LOG.warning("cover not found id=%s", cover_id)
        return
    if cover["status"] == "ready":
        LOG.info("cover already ready id=%s", cover_id)
        return

    attempt = mark_processing(conn, cover_id)
    if attempt is None:
        # Exhausted attempts or missing row.
        current = fetch_cover(conn, cover_id)
        if current and current["attempt_count"] >= MAX_ATTEMPTS and current["status"] != "ready":
            mark_failed(conn, cover_id, f"exceeded max attempts ({MAX_ATTEMPTS})")
            LOG.error("cover exhausted attempts id=%s", cover_id)
        else:
            LOG.info("skip cover id=%s status=%s", cover_id, cover["status"])
        return

    LOG.info(
        "processing cover id=%s song=%s artist=%s attempt=%s mode=%s",
        cover_id, cover["source_song_id"], cover["artist_slug"], attempt, MODE,
    )

    job_dir = work_dir / f"cover-{cover_id}-{attempt}"
    job_dir.mkdir(parents=True, exist_ok=True)
    source_path = job_dir / "source.mp3"
    output_path = job_dir / "output.mp3"

    try:
        download_source(cover["source_song_id"], source_path)
        slug = cover["artist_slug"]
        model_path = models_dir / slug / "model.pth"
        index_path = models_dir / slug / "index.index"
        if MODE == "rvc" and not model_path.exists():
            # Permanent failure — retrying will never help until a model is installed.
            raise PermanentJobError(
                f"RVC model missing for {slug}; run scripts/download-voice-models.sh "
                f"or place model.pth in {models_dir / slug}"
            )
        pitch = load_artist_pitch(models_dir, slug, int(cover["pitch_default"] or 0))
        update_progress(conn, cover_id, 3, "downloading", 0)

        def on_progress(pct: int, stage: str, eta: int) -> None:
            update_progress(conn, cover_id, pct, stage, eta)

        process_cover(
            mode=MODE,
            source_path=source_path,
            output_path=output_path,
            model_path=model_path if model_path.exists() else None,
            index_path=index_path if index_path.exists() else None,
            pitch=pitch,
            work_dir=job_dir,
            on_progress=on_progress,
        )
        update_progress(conn, cover_id, 99, "uploading", 5)
        object_key = upload_result(minio_client, bucket, cover_id, output_path)
        mark_ready(conn, cover_id, object_key)
        LOG.info("cover ready id=%s key=%s", cover_id, object_key)
    except PermanentJobError as exc:
        LOG.error("cover failed permanently id=%s error=%s", cover_id, exc)
        mark_failed(conn, cover_id, str(exc))
    except Exception as exc:  # noqa: BLE001 — job isolation
        LOG.error("cover failed id=%s error=%s", cover_id, exc)
        LOG.debug(traceback.format_exc())
        msg = str(exc)
        permanent = (
            "RVC base models missing" in msg
            or "IncompleteRead" in msg
            or "ChunkedEncodingError" in msg
            or "Connection aborted" in msg
            or "RemoteDisconnected" in msg
        )
        mark_failed(conn, cover_id, msg)
        if permanent:
            LOG.error("cover failed permanently (network/model) id=%s", cover_id)
            return
        if attempt < MAX_ATTEMPTS:
            # Soft retry via outer requeue.
            raise
    finally:
        # Keep disk tidy; stems/temp live under job_dir.
        try:
            for path in sorted(job_dir.rglob("*"), reverse=True):
                if path.is_file():
                    path.unlink(missing_ok=True)
                elif path.is_dir():
                    path.rmdir()
            job_dir.rmdir()
        except OSError:
            pass


def parse_job(payload: str) -> int | None:
    try:
        data = json.loads(payload)
        cover_id = int(data.get("cover_id", 0))
        return cover_id if cover_id > 0 else None
    except (json.JSONDecodeError, TypeError, ValueError):
        return None


def handle_signal(signum: int, _frame: Any) -> None:
    global _running
    LOG.info("shutdown signal=%s", signum)
    _running = False


def main() -> int:
    setup_logging()
    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)

    models_dir = Path(env("VOICE_MODELS_DIR", "/models"))
    work_root = Path(env("VOICE_WORKER_TMPDIR", tempfile.gettempdir())) / "melonet-voice"
    work_root.mkdir(parents=True, exist_ok=True)

    LOG.info(
        "starting voice-worker mode=%s models=%s concurrency=%s max_attempts=%s",
        MODE, models_dir, CONCURRENCY, MAX_ATTEMPTS,
    )

    redis_client = connect_redis()
    db = connect_db()
    minio_client, bucket = connect_minio()

    while _running:
        try:
            item = redis_client.brpop(QUEUE_KEY, timeout=BRPOP_TIMEOUT_SEC)
        except RedisTimeoutError:
            # Empty queue / idle socket — keep polling.
            continue
        if not item:
            continue
        _, payload = item
        cover_id = parse_job(payload)
        if cover_id is None:
            LOG.warning("invalid job payload=%s", payload)
            continue
        try:
            handle_job(db, minio_client, bucket, cover_id, models_dir, work_root)
        except Exception as exc:  # noqa: BLE001
            LOG.error("job crashed id=%s error=%s", cover_id, exc)
            # Soft retry via queue if attempts remain.
            try:
                cover = fetch_cover(db, cover_id)
                if cover and cover["attempt_count"] < MAX_ATTEMPTS and cover["status"] != "ready":
                    redis_client.lpush(QUEUE_KEY, json.dumps({"cover_id": cover_id}))
                    LOG.info("requeued cover id=%s", cover_id)
            except Exception:  # noqa: BLE001
                LOG.exception("failed to requeue cover id=%s", cover_id)
            time.sleep(1)

    db.close()
    LOG.info("voice-worker stopped")
    return 0


if __name__ == "__main__":
    sys.exit(main())
