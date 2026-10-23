from __future__ import annotations

import os
from pathlib import Path


class Settings:
    def __init__(self) -> None:
        self.host: str = os.getenv("STT_HOST", "127.0.0.1")
        self.port: int = int(os.getenv("STT_PORT", "8091"))
        
        # Legacy/default model
        self.whisper_model: str = os.getenv("WHISPER_MODEL", "base").strip() or "base"
        self.whisper_device: str = os.getenv("WHISPER_DEVICE", "cpu").strip() or "cpu"
        self.whisper_compute_type: str = os.getenv("WHISPER_COMPUTE_TYPE", "int8").strip() or "int8"
        
        # Profile-aware Whisper configuration
        self.whisper_fast_model: str = os.getenv("WHISPER_FAST_MODEL", self.whisper_model).strip() or "base"
        self.whisper_quality_model: str = os.getenv("WHISPER_QUALITY_MODEL", "base").strip() or "base"
        self.whisper_fast_compute_type: str = os.getenv("WHISPER_FAST_COMPUTE_TYPE", self.whisper_compute_type).strip() or "int8"
        self.whisper_quality_compute_type: str = os.getenv("WHISPER_QUALITY_COMPUTE_TYPE", self.whisper_compute_type).strip() or "int8"
        
        # Memory safety: maximum loaded models in cache
        self.stt_max_loaded_models: int = int(os.getenv("STT_MAX_LOADED_MODELS", "2"))
        
        # Safe fallback behavior
        self.allow_quality_stt_fallback: bool = os.getenv("ALLOW_QUALITY_STT_FALLBACK", "true").lower() in ("true", "1", "yes")
        
        self.max_audio_bytes: int = int(os.getenv("MAX_AUDIO_BYTES", str(4 * 1024 * 1024)))
        self.max_audio_seconds: float = float(os.getenv("MAX_AUDIO_SECONDS", "60.0"))


settings = Settings()
