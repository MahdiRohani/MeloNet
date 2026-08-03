# Python 3.10: fairseq (rvc-python dep) breaks on 3.11+ dataclasses.
FROM python:3.10-slim-bookworm

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ffmpeg \
        git \
        curl \
        ca-certificates \
        build-essential \
        libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

# CPU PyTorch first so rvc-python/demucs do not pull a CUDA wheel.
ARG TORCH_INDEX=https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir \
      torch==2.4.1+cpu torchaudio==2.4.1+cpu \
      --index-url ${TORCH_INDEX}

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY worker.py pipeline.py ./

# rvc-python mutates its own package configs at runtime (fp32 patch) and downloads
# hubert/rmvpe weights under site-packages — those paths must be writable.
RUN mkdir -p /models /tmp/melonet-voice /home/voice/.cache \
    && (getent group voice >/dev/null || groupadd -g 1000 voice) \
    && (id -u voice >/dev/null 2>&1 || useradd -m -u 1000 -g voice voice) \
    && chmod -R a+rwX /usr/local/lib/python3.10/site-packages/rvc_python \
    && chown -R voice:voice /app /models /tmp/melonet-voice /home/voice

USER voice

ENV VOICE_WORKER_MODE=rvc \
    VOICE_MODELS_DIR=/models \
    VOICE_WORKER_TMPDIR=/tmp/melonet-voice \
    VOICE_RVC_DEVICE=cpu \
    VOICE_DEMUCS_DEVICE=cpu \
    HOME=/home/voice \
    HF_HOME=/home/voice/.cache/huggingface \
    TORCH_HOME=/home/voice/.cache/torch \
    XDG_CACHE_HOME=/home/voice/.cache \
    PYTHONUNBUFFERED=1

CMD ["python", "-u", "worker.py"]
