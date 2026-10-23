# TRON AI — Standalone Speech-to-Text (STT) Worker

Dedicated ML microservice wrapping `faster-whisper` for audio transcription.

## Endpoints

- `GET /health` — Worker health and active Whisper model/device details
- `POST /v1/transcribe` — Accepts multipart audio (`file`) and optional `language` code (e.g., `ja`, `en`), returns `{"text": "...", "duration": ..., "language": "..."}`

## Runtime Details

- Port: 8091 (`http://127.0.0.1:8091`)
- Default Device: `cpu`
- Default Compute Type: `int8`
- Default Whisper Model: `base`
- Supported Audio Formats: WAV, MP3, M4A, WebM, OGG, FLAC
- Max Audio Size: 4MB
- Max Audio Duration: 60s

## GPU Optimization (RTX 4060)

When deploying on NVIDIA CUDA GPU:
```env
WHISPER_DEVICE=cuda
WHISPER_COMPUTE_TYPE=float16
```
