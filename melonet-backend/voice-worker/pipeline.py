"""Audio pipeline for voice covers.

Modes:
  stub — copy/re-encode source to output (E2E without Demucs/RVC models)
  rvc  — Demucs stem split → RVC inference → mix vocals + instrumental

Long tracks are truncated (VOICE_MAX_SOURCE_SEC, default 60) so CPU RVC stays
fast enough and does not OOM on long songs.

Speed defaults (CPU-friendly):
  - Demucs shifts=0, in-process Separator reuse
  - RVC f0method=harvest, lower index_rate, in-process RVCInference reuse
"""

from __future__ import annotations

import logging
import os
import shutil
import subprocess
import threading
import time
from collections.abc import Callable
from pathlib import Path

LOG = logging.getLogger("voice-worker.pipeline")

ProgressCb = Callable[[int, str, int], None]

_demucs_lock = threading.Lock()
_demucs_separator = None
_demucs_key: tuple[str, str, int] | None = None

_rvc_lock = threading.Lock()
_rvc_engine = None
_rvc_model_path: str | None = None


def max_source_seconds() -> int:
    try:
        return max(15, int(os.getenv("VOICE_MAX_SOURCE_SEC", "60")))
    except ValueError:
        return 60


def _configure_torch_threads() -> None:
    try:
        import torch

        threads = int(os.getenv("VOICE_TORCH_THREADS", "0") or "0")
        if threads <= 0:
            threads = max(1, min(4, os.cpu_count() or 2))
        torch.set_num_threads(threads)
        torch.set_num_interop_threads(1)
        LOG.info("torch threads=%s", threads)
    except Exception:  # noqa: BLE001
        LOG.debug("torch thread config skipped", exc_info=True)


def process_cover(
    *,
    mode: str,
    source_path: Path,
    output_path: Path,
    model_path: Path | None,
    index_path: Path | None,
    pitch: int,
    work_dir: Path,
    on_progress: ProgressCb | None = None,
) -> None:
    mode = (mode or "stub").strip().lower()
    if mode == "rvc":
        _process_rvc(
            source_path=source_path,
            output_path=output_path,
            model_path=model_path,
            index_path=index_path,
            pitch=pitch,
            work_dir=work_dir,
            on_progress=on_progress,
        )
        return
    _report(on_progress, 50, "encoding", 30)
    _process_stub(source_path=source_path, output_path=output_path)
    _report(on_progress, 100, "done", 0)


def _report(cb: ProgressCb | None, pct: int, stage: str, eta: int) -> None:
    if cb is None:
        return
    try:
        cb(max(0, min(100, pct)), stage, max(0, eta))
    except Exception:  # noqa: BLE001 — never break the job on progress IO
        LOG.debug("progress callback failed", exc_info=True)


def _process_stub(*, source_path: Path, output_path: Path) -> None:
    """Passthrough that still produces a playable MP3 when ffmpeg is available."""
    ffmpeg = shutil.which("ffmpeg")
    if ffmpeg:
        cmd = [
            ffmpeg, "-y", "-i", str(source_path),
            "-codec:a", "libmp3lame", "-b:a", "160k",
            str(output_path),
        ]
        LOG.info("stub re-encode via ffmpeg")
        subprocess.run(cmd, check=True, capture_output=True)
        return
    shutil.copyfile(source_path, output_path)
    LOG.info("stub copy (ffmpeg not found)")


class _StageTicker:
    """Soft-advance progress while a long subprocess runs."""

    def __init__(
        self,
        on_progress: ProgressCb | None,
        *,
        stage: str,
        start_pct: int,
        end_pct: int,
        eta_sec: int,
    ) -> None:
        self._cb = on_progress
        self._stage = stage
        self._start = start_pct
        self._end = end_pct
        self._eta = max(5, eta_sec)
        self._stop = threading.Event()
        self._thread: threading.Thread | None = None

    def __enter__(self) -> _StageTicker:
        _report(self._cb, self._start, self._stage, self._eta)
        if self._cb is not None:
            self._thread = threading.Thread(target=self._run, name="progress-ticker", daemon=True)
            self._thread.start()
        return self

    def __exit__(self, *args: object) -> None:
        self._stop.set()
        if self._thread is not None:
            self._thread.join(timeout=2)
        _report(self._cb, self._end, self._stage, 0)

    def _run(self) -> None:
        t0 = time.monotonic()
        while not self._stop.wait(3.0):
            elapsed = time.monotonic() - t0
            frac = min(0.92, elapsed / self._eta)
            pct = int(self._start + (self._end - self._start) * frac)
            remaining = max(0, int(self._eta - elapsed))
            _report(self._cb, pct, self._stage, remaining)


def _probe_duration_sec(path: Path) -> float:
    ffprobe = shutil.which("ffprobe")
    if not ffprobe:
        return float(max_source_seconds())
    try:
        out = subprocess.check_output(
            [
                ffprobe, "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                str(path),
            ],
            text=True,
        ).strip()
        return max(1.0, float(out))
    except (subprocess.CalledProcessError, ValueError):
        return float(max_source_seconds())


def _process_rvc(
    *,
    source_path: Path,
    output_path: Path,
    model_path: Path | None,
    index_path: Path | None,
    pitch: int,
    work_dir: Path,
    on_progress: ProgressCb | None,
) -> None:
    if model_path is None or not model_path.exists():
        raise RuntimeError(
            "RVC model not found; place model.pth under VOICE_MODELS_DIR/<slug>/ "
            "or run scripts/download-voice-models.sh"
        )

    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        raise RuntimeError("ffmpeg is required for RVC mode")

    _configure_torch_threads()

    stems_dir = work_dir / "stems"
    stems_dir.mkdir(parents=True, exist_ok=True)

    max_sec = max_source_seconds()
    source_wav = work_dir / "source.wav"
    # Truncate early — long songs OOM / take 30+ minutes on CPU.
    LOG.info("normalizing source max_sec=%s", max_sec)
    _report(on_progress, 5, "preparing", max(20, int(max_sec * 4)))
    subprocess.run(
        [
            ffmpeg, "-y", "-i", str(source_path),
            "-t", str(max_sec),
            "-ac", "2", "-ar", "44100",
            "-sample_fmt", "s16",
            str(source_wav),
        ],
        check=True,
        capture_output=True,
    )
    try:
        source_path.unlink(missing_ok=True)
    except OSError:
        pass

    clip_sec = _probe_duration_sec(source_wav)
    # Faster CPU estimates after harvest + shifts=0 + in-process reuse.
    demucs_eta = max(15, int(clip_sec * 0.7))
    rvc_eta = max(25, int(clip_sec * 4.0))

    LOG.info("running demucs clip_sec=%.1f", clip_sec)
    with _StageTicker(
        on_progress,
        stage="separating",
        start_pct=10,
        end_pct=40,
        eta_sec=demucs_eta + rvc_eta,
    ):
        vocals, instrumental = _run_demucs(source_wav, stems_dir)

    try:
        source_wav.unlink(missing_ok=True)
    except OSError:
        pass

    converted = work_dir / "converted_vocals.wav"
    with _StageTicker(
        on_progress,
        stage="converting",
        start_pct=42,
        end_pct=88,
        eta_sec=rvc_eta,
    ):
        _run_rvc(
            vocals_path=vocals,
            output_path=converted,
            model_path=model_path,
            index_path=index_path,
            pitch=pitch,
        )
    if not converted.exists():
        raise RuntimeError("RVC did not produce converted vocals")

    # Free demucs stems ASAP after conversion.
    try:
        vocals.unlink(missing_ok=True)
    except OSError:
        pass

    _report(on_progress, 92, "mixing", 10)
    mix_cmd = [
        ffmpeg, "-y",
        "-i", str(converted),
        "-i", str(instrumental),
        "-filter_complex",
        "[0:a]volume=1.15[v];[1:a]volume=0.95[i];[v][i]amix=inputs=2:duration=longest:dropout_transition=0[out]",
        "-map", "[out]",
        "-ac", "2",
        "-ar", "44100",
        "-codec:a", "libmp3lame", "-b:a", "160k",
        str(output_path),
    ]
    LOG.info("mixing stems")
    subprocess.run(mix_cmd, check=True, capture_output=True)
    _report(on_progress, 98, "uploading", 5)


def _run_demucs(source_wav: Path, stems_dir: Path) -> tuple[Path, Path]:
    """Separate vocals with a cached in-process Demucs Separator when possible."""
    model_name = os.getenv("VOICE_DEMUCS_MODEL", "htdemucs")
    device = os.getenv("VOICE_DEMUCS_DEVICE", "cpu")
    try:
        shifts = max(0, int(os.getenv("VOICE_DEMUCS_SHIFTS", "0")))
    except ValueError:
        shifts = 0

    try:
        vocals, instrumental = _run_demucs_inplace(
            source_wav=source_wav,
            stems_dir=stems_dir,
            model_name=model_name,
            device=device,
            shifts=shifts,
        )
        return vocals, instrumental
    except Exception as exc:  # noqa: BLE001
        LOG.warning("in-process demucs failed (%s); falling back to CLI", exc)

    import sys

    demucs_cmd = [
        sys.executable, "-m", "demucs",
        "--two-stems=vocals",
        "-n", model_name,
        "-d", device,
        "--shifts", str(shifts),
        "-o", str(stems_dir),
        str(source_wav),
    ]
    subprocess.run(demucs_cmd, check=True)
    return _find_demucs_stems(stems_dir, model_name)


def _run_demucs_inplace(
    *,
    source_wav: Path,
    stems_dir: Path,
    model_name: str,
    device: str,
    shifts: int,
) -> tuple[Path, Path]:
    global _demucs_separator, _demucs_key

    import torch
    from demucs.api import Separator, save_audio

    key = (model_name, device, shifts)
    with _demucs_lock:
        if _demucs_separator is None or _demucs_key != key:
            LOG.info("loading demucs model=%s device=%s shifts=%s", model_name, device, shifts)
            _demucs_separator = Separator(
                model=model_name,
                device=device,
                shifts=shifts,
                split=True,
                overlap=0.25,
                progress=False,
            )
            _demucs_key = key
        separator = _demucs_separator

    origin, separated = separator.separate_audio_file(str(source_wav))
    del origin

    out_dir = stems_dir / model_name / source_wav.stem
    out_dir.mkdir(parents=True, exist_ok=True)
    vocals = out_dir / "vocals.wav"
    instrumental = out_dir / "no_vocals.wav"

    if "vocals" not in separated:
        raise RuntimeError(f"demucs stems missing vocals: {list(separated)}")
    sr = getattr(separator, "samplerate", 44100)
    save_audio(separated["vocals"].cpu(), str(vocals), samplerate=sr)
    if "no_vocals" in separated:
        save_audio(separated["no_vocals"].cpu(), str(instrumental), samplerate=sr)
    else:
        other = None
        for name, tensor in separated.items():
            if name == "vocals":
                continue
            other = tensor if other is None else other + tensor
        if other is None:
            raise RuntimeError("demucs produced no instrumental stem")
        save_audio(other.cpu(), str(instrumental), samplerate=sr)

    del separated
    if device.startswith("cuda") and torch.cuda.is_available():
        torch.cuda.empty_cache()

    return vocals, instrumental


def _find_demucs_stems(stems_dir: Path, model_name: str) -> tuple[Path, Path]:
    track_dirs = list((stems_dir / model_name).glob("*"))
    if not track_dirs:
        track_dirs = [p.parent for p in stems_dir.rglob("vocals.wav")]
    if not track_dirs:
        raise RuntimeError("demucs produced no stems")
    track_dir = track_dirs[0]
    vocals = track_dir / "vocals.wav"
    instrumental = track_dir / "no_vocals.wav"
    if not vocals.exists() or not instrumental.exists():
        raise RuntimeError(f"missing demucs stems in {track_dir}")
    return vocals, instrumental


def _run_rvc(
    *,
    vocals_path: Path,
    output_path: Path,
    model_path: Path,
    index_path: Path | None,
    pitch: int,
) -> None:
    """Run RVC via in-process RVCInference (cached) or CLI fallback."""
    rvc_cmd_template = os.getenv("VOICE_RVC_CMD", "").strip()
    if rvc_cmd_template:
        cmd = (
            rvc_cmd_template
            .replace("{model}", str(model_path))
            .replace("{index}", str(index_path) if index_path else "")
            .replace("{input}", str(vocals_path))
            .replace("{output}", str(output_path))
            .replace("{pitch}", str(pitch))
        )
        LOG.info("running custom RVC cmd")
        subprocess.run(cmd, check=True, shell=True)
        return

    device = os.getenv("VOICE_RVC_DEVICE", "cpu")
    # harvest is much faster than rmvpe on CPU with acceptable quality for short clips.
    method = os.getenv("VOICE_RVC_METHOD", "harvest")
    version = os.getenv("VOICE_RVC_VERSION", "v2")
    index_rate = os.getenv("VOICE_RVC_INDEX_RATE", "0.5")

    _ensure_rvc_base_models()

    try:
        _run_rvc_inplace(
            vocals_path=vocals_path,
            output_path=output_path,
            model_path=model_path,
            index_path=index_path,
            pitch=pitch,
            device=device,
            method=method,
            index_rate=float(index_rate),
        )
        return
    except Exception as exc:  # noqa: BLE001
        LOG.warning("in-process RVC failed (%s); falling back to CLI", exc)

    import sys

    cmd = [
        sys.executable, "-m", "rvc_python", "cli",
        "-i", str(vocals_path),
        "-o", str(output_path),
        "-mp", str(model_path),
        "-de", device,
        "-me", method,
        "-v", version,
        "-pi", str(pitch),
        "-ir", index_rate,
    ]
    if index_path is not None and index_path.exists() and float(index_rate) > 0:
        cmd.extend(["-ip", str(index_path)])

    LOG.info("running rvc-python CLI device=%s method=%s pitch=%s", device, method, pitch)
    subprocess.run(cmd, check=True)


def _rvc_base_model_dir() -> Path:
    override = os.getenv("RVC_BASE_MODELS_DIR", "").strip()
    if override:
        return Path(override)
    import rvc_python

    return Path(rvc_python.__file__).resolve().parent / "base_model"


def _ensure_rvc_base_models() -> None:
    """Make sure hubert/rmvpe weights exist so rvc-python does not re-download mid-job."""
    required = ("hubert_base.pt", "rmvpe.pt")
    base_dir = _rvc_base_model_dir()
    base_dir.mkdir(parents=True, exist_ok=True)

    # Also keep site-packages copy in sync when using a mounted cache dir.
    try:
        import rvc_python

        pkg_dir = Path(rvc_python.__file__).resolve().parent / "base_model"
        pkg_dir.mkdir(parents=True, exist_ok=True)
    except Exception:  # noqa: BLE001
        pkg_dir = base_dir

    missing = [name for name in required if not (base_dir / name).exists()]
    if missing:
        raise RuntimeError(
            "RVC base models missing: "
            + ", ".join(missing)
            + f". Run scripts/ensure-rvc-base-models.sh into {base_dir}"
        )

    # Soft-link/copy into package dir if RVC looks there instead of RVC_BASE_MODELS_DIR.
    if pkg_dir.resolve() != base_dir.resolve():
        for name in required:
            src = base_dir / name
            dst = pkg_dir / name
            if dst.exists():
                continue
            try:
                os.link(src, dst)
            except OSError:
                try:
                    if not dst.exists():
                        import shutil

                        shutil.copy2(src, dst)
                except OSError as exc:
                    LOG.warning("could not seed %s into package base_model: %s", name, exc)

    # rvc-python's download_rvc_models() also tries rmvpe.onnx (~hundreds of MB) even when
    # f0method=harvest/pm and never uses it. Skip that download so jobs don't stall at ~84%.
    _patch_rvc_optional_downloads()


def _patch_rvc_optional_downloads() -> None:
    try:
        from rvc_python import download_model
    except Exception:  # noqa: BLE001
        return

    if getattr(download_model.download_rvc_models, "_melonet_patched", False):
        return

    original = download_model.download_rvc_models

    def _download_without_onnx(this_dir: str) -> None:
        folder = os.path.join(this_dir, "base_model")
        os.makedirs(folder, exist_ok=True)
        # Pretend onnx already exists so upstream skip-download path is used.
        onnx_path = os.path.join(folder, "rmvpe.onnx")
        if not os.path.exists(onnx_path):
            # Zero-byte marker: harvest/pm never load onnx; only rmvpe.onnx path would.
            with open(onnx_path, "wb") as fh:
                fh.write(b"")
        return original(this_dir)

    _download_without_onnx._melonet_patched = True  # type: ignore[attr-defined]
    download_model.download_rvc_models = _download_without_onnx  # type: ignore[assignment]
    LOG.info("patched rvc-python to skip rmvpe.onnx download")


def _run_rvc_inplace(
    *,
    vocals_path: Path,
    output_path: Path,
    model_path: Path,
    index_path: Path | None,
    pitch: int,
    device: str,
    method: str,
    index_rate: float,
) -> None:
    global _rvc_engine, _rvc_model_path

    from rvc_python.infer import RVCInference

    _ensure_rvc_base_models()

    model_key = str(model_path.resolve())
    with _rvc_lock:
        if _rvc_engine is None:
            LOG.info("creating RVCInference device=%s", device)
            _rvc_engine = RVCInference(device=device)
            _rvc_model_path = None

        if _rvc_model_path != model_key:
            LOG.info("loading RVC model=%s", model_path.name)
            index_arg = str(index_path) if index_path and index_path.exists() and index_rate > 0 else None
            if index_arg:
                try:
                    _rvc_engine.load_model(model_key, index_path=index_arg)
                except TypeError:
                    _rvc_engine.load_model(model_key)
            else:
                _rvc_engine.load_model(model_key)
            _rvc_model_path = model_key

        params = {
            "f0method": method,
            "f0up_key": int(pitch),
            "index_rate": float(index_rate),
            "filter_radius": int(os.getenv("VOICE_RVC_FILTER_RADIUS", "3")),
            "resample_sr": 0,
            "rms_mix_rate": float(os.getenv("VOICE_RVC_RMS_MIX_RATE", "0.25")),
            "protect": float(os.getenv("VOICE_RVC_PROTECT", "0.33")),
        }
        try:
            _rvc_engine.set_params(**params)
        except TypeError:
            # Older rvc-python variants accept a dict or different kw names.
            try:
                _rvc_engine.set_params(params)
            except Exception:  # noqa: BLE001
                LOG.debug("set_params unsupported; using engine defaults", exc_info=True)

        LOG.info("running in-process RVC method=%s pitch=%s ir=%s", method, pitch, index_rate)
        _rvc_engine.infer_file(str(vocals_path), str(output_path))
