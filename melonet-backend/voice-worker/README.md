# Voice Cover Worker

Consumes Redis list `voice_cover:jobs`, runs Demucs → RVC → mix, uploads to MinIO,
and updates `voice_covers` in Postgres.

## Modes

| `VOICE_WORKER_MODE` | Behavior |
|---------------------|----------|
| `rvc` (default) | Real conversion — needs `data/voice-models/<slug>/model.pth` |
| `stub` | Copy/re-encode source (no voice change) |

## Setup models

```bash
cd melonet-backend
# Artist voice packs (shadmehr, ebi, …)
make download-voice-models
# One-time hubert + rmvpe base weights (~350MB). Without these, jobs stall ~80% while
# rvc-python tries (and often fails) to download mid-conversion.
./scripts/ensure-rvc-base-models.sh
```

Currently scripted public packs:

- `morteza-pashaei`, `shadmehr`, `hayedeh`, `mahasti`, `mohsen-chavoshi`, `ebi`

For the rest (`moein`, `googoosh`, `mohsen-yeganeh`, `siavash-ghomayshi`), drop a community `.pth` (and optional `.index`) into:

```text
data/voice-models/<slug>/model.pth
data/voice-models/<slug>/index.index   # optional
```

## Run

```bash
# After models exist:
VOICE_WORKER_MODE=rvc make docker-up
```

CPU inference is capped to ~60s clips (`VOICE_MAX_SOURCE_SEC`). Speed knobs:

| Env | Default | Effect |
|-----|---------|--------|
| `VOICE_RVC_METHOD` | `harvest` | Faster f0 than `rmvpe` on CPU |
| `VOICE_RVC_INDEX_RATE` | `0.5` | Lower = faster (less index search) |
| `VOICE_DEMUCS_SHIFTS` | `0` | Skip Demucs random shifts |
| `VOICE_TORCH_THREADS` | auto ≤4 | Torch intra-op threads |

The worker keeps Demucs/RVC models warm in-process across jobs. GPU: set `VOICE_RVC_DEVICE=cuda:0` and `VOICE_DEMUCS_DEVICE=cuda`.
