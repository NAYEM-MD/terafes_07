from __future__ import annotations

import asyncio
import io
import logging
import os
import tempfile
import threading
import wave
from pathlib import Path
from typing import Optional, Tuple

from app.config import settings
from app.errors import STTError

logger = logging.getLogger("stt_worker")

_ALLOWED_EXTENSIONS = {".wav", ".mp3", ".m4a", ".webm", ".ogg", ".flac"}
_ALLOWED_MIME_TYPES = {
    "",
    "application/octet-stream",
    "audio/wav",
    "audio/x-wav",
    "audio/wave",
    "audio/mpeg",
    "audio/mp3",
    "audio/webm",
    "audio/ogg",
    "audio/mp4",
    "audio/x-m4a",
    "audio/flac",
    "audio/x-flac",
}


class WhisperModelRegistry:
    """Thread-safe registry managing Whisper models for FAST and QUALITY profiles."""

    def __init__(self) -> None:
        self._models: dict[str, object] = {}
        self._lock = threading.Lock()

    def reset(self) -> None:
        with self._lock:
            self._models.clear()

    def get_model(self, profile: str = "FAST") -> Tuple[object, str, str]:
        """
        Returns (model_instance, effective_profile, model_name).
        Handles lazy initialization, memory safety limits, and graceful fallback.
        """
        norm_profile = (profile or "FAST").strip().upper()
        if norm_profile not in ("FAST", "QUALITY"):
            norm_profile = "FAST"

        with self._lock:
            if norm_profile in self._models:
                target_model_name = (
                    settings.whisper_quality_model if norm_profile == "QUALITY" else settings.whisper_fast_model
                )
                return self._models[norm_profile], norm_profile, target_model_name

            # Evict if max loaded models reached
            if len(self._models) >= settings.stt_max_loaded_models:
                evict_key = next(iter(self._models.keys()))
                logger.info("Evicting STT model '%s' from cache to preserve memory limit", evict_key)
                del self._models[evict_key]

            from faster_whisper import WhisperModel

            if norm_profile == "QUALITY":
                target_model = settings.whisper_quality_model
                compute_type = settings.whisper_quality_compute_type
                try:
                    logger.info(
                        "Initializing QUALITY WhisperModel model=%s device=%s compute=%s",
                        target_model,
                        settings.whisper_device,
                        compute_type,
                    )
                    model = WhisperModel(
                        target_model,
                        device=settings.whisper_device,
                        compute_type=compute_type,
                    )
                    self._models["QUALITY"] = model
                    return model, "QUALITY", target_model
                except Exception as exc:
                    if settings.allow_quality_stt_fallback:
                        logger.warning(
                            "QUALITY Whisper model '%s' failed to load: %s. Falling back truthfully to FAST model '%s'",
                            target_model,
                            exc,
                            settings.whisper_fast_model,
                        )
                        # Fallback to FAST
                        norm_profile = "FAST"
                    else:
                        logger.error("QUALITY Whisper model failed and fallback disabled: %s", exc)
                        raise STTError(503, "Quality STT model is unavailable") from exc

            # FAST model loading
            target_model = settings.whisper_fast_model
            compute_type = settings.whisper_fast_compute_type
            if "FAST" not in self._models:
                logger.info(
                    "Initializing FAST WhisperModel model=%s device=%s compute=%s",
                    target_model,
                    settings.whisper_device,
                    compute_type,
                )
                model = WhisperModel(
                    target_model,
                    device=settings.whisper_device,
                    compute_type=compute_type,
                )
                self._models["FAST"] = model
            return self._models["FAST"], norm_profile, target_model


_registry = WhisperModelRegistry()


def reset_model() -> None:
    _registry.reset()


def get_model(profile: str = "FAST"):
    model, _, _ = _registry.get_model(profile)
    return model


def _extension(filename: str) -> str:
    return Path(filename or "").suffix.lower()


def _matches_signature(data: bytes, extension: str) -> bool:
    if extension == ".wav":
        return len(data) >= 12 and data[:4] == b"RIFF" and data[8:12] == b"WAVE"
    if extension == ".ogg":
        return data.startswith(b"OggS")
    if extension == ".webm":
        return data.startswith(b"\x1a\x45\xdf\xa3")
    if extension == ".flac":
        return data.startswith(b"fLaC")
    if extension == ".mp3":
        return data.startswith(b"ID3") or (len(data) > 2 and data[0] == 0xFF and (data[1] & 0xE0) == 0xE0)
    if extension == ".m4a":
        return len(data) > 11 and data[4:8] == b"ftyp"
    return False


def _wav_duration(data: bytes) -> float:
    try:
        with wave.open(io.BytesIO(data), "rb") as audio:
            rate = audio.getframerate()
            frames = audio.getnframes()
            if rate <= 0 or frames < 0 or audio.getnchannels() < 1:
                raise STTError(400, "The audio file is invalid")
            return frames / float(rate)
    except STTError:
        raise
    except Exception as exc:
        raise STTError(400, "The audio file is invalid") from exc


def _container_duration(data: bytes) -> float:
    try:
        import av
    except ImportError as exc:
        raise STTError(500, "Audio decoder library not available") from exc
    try:
        container = av.open(io.BytesIO(data))
    except Exception as exc:
        raise STTError(400, "The audio file is invalid") from exc
    try:
        stream = next((item for item in container.streams if item.type == "audio"), None)
        if stream is None:
            raise STTError(400, "The audio file is invalid")
        if stream.duration is not None and stream.time_base is not None:
            return float(stream.duration * stream.time_base)
        if container.duration:
            return float(container.duration) / 1_000_000
        frames = 0
        rate = stream.sample_rate or 0
        for frame in container.decode(stream):
            frames += frame.samples
            rate = frame.sample_rate or rate
        if rate <= 0 or frames <= 0:
            raise STTError(400, "The audio file is invalid")
        return frames / float(rate)
    except STTError:
        raise
    except Exception as exc:
        raise STTError(400, "The audio file is invalid") from exc
    finally:
        container.close()


def validate_audio(filename: str, content_type: str, data: bytes) -> float:
    if not data:
        raise STTError(400, "The audio file is empty")
    if len(data) > settings.max_audio_bytes:
        raise STTError(413, f"Audio exceeds the {settings.max_audio_bytes // (1024*1024)}MB limit")

    ext = _extension(filename)
    if not ext and content_type:
        type_clean = content_type.split(";", 1)[0].strip().lower()
        if "wav" in type_clean:
            ext = ".wav"
        elif "webm" in type_clean:
            ext = ".webm"
        elif "mpeg" in type_clean or "mp3" in type_clean:
            ext = ".mp3"
        elif "ogg" in type_clean:
            ext = ".ogg"
        elif "flac" in type_clean:
            ext = ".flac"
        elif "m4a" in type_clean or "mp4" in type_clean:
            ext = ".m4a"

    if not ext:
        ext = ".wav"

    media_type = (content_type or "").split(";", 1)[0].strip().lower()
    if ext not in _ALLOWED_EXTENSIONS or (media_type and media_type not in _ALLOWED_MIME_TYPES):
        raise STTError(415, "Only short WAV, MP3, M4A, WebM, Ogg, or FLAC audio is accepted")

    if not _matches_signature(data, ext):
        raise STTError(400, "The audio file is invalid")

    duration = _wav_duration(data) if ext == ".wav" else _container_duration(data)
    if duration <= 0:
        raise STTError(400, "The audio file is invalid")
    if duration > settings.max_audio_seconds:
        raise STTError(413, f"Audio exceeds the {int(settings.max_audio_seconds)} second limit")

    return round(duration, 3)


def _read_waveform(path: str):
    import av
    import numpy as np

    container = av.open(path)
    resampler = av.audio.resampler.AudioResampler(format="s16", layout="mono", rate=16000)
    chunks = []
    try:
        stream = next((item for item in container.streams if item.type == "audio"), None)
        if stream is None:
            raise STTError(400, "The audio file is invalid")
        frames = list(container.decode(stream))
        frames.append(None)
        for frame in frames:
            converted = resampler.resample(frame)
            converted_frames = converted if isinstance(converted, list) else [converted]
            for item in converted_frames:
                if item is not None:
                    chunks.append(item.to_ndarray())
    except STTError:
        raise
    except Exception as exc:
        raise STTError(400, "The audio file is invalid") from exc
    finally:
        container.close()

    if not chunks:
        raise STTError(400, "The audio file is invalid")
    audio = np.concatenate([chunk.reshape(-1) for chunk in chunks]).astype(np.float32)
    return audio / 32768.0


def _transcribe_sync(path: str, language: Optional[str], profile: str = "FAST") -> tuple[str, str, str, str]:
    model, effective_profile, model_name = _registry.get_model(profile)
    waveform = _read_waveform(path)
    lang_param = language if language and language != "auto" else None
    segments, info = model.transcribe(
        waveform,
        language=lang_param,
        task="transcribe",
        vad_filter=True,
        beam_size=1,
    )
    detected_lang = info.language if info and info.language else (language or "ja")
    text = "".join(segment.text for segment in segments).strip()
    return text, detected_lang, effective_profile, model_name


async def transcribe(
    filename: str,
    content_type: str,
    data: bytes,
    language: Optional[str] = "ja",
    profile: Optional[str] = "FAST",
) -> tuple[str, float, str, str, str]:
    duration = validate_audio(filename, content_type, data)
    ext = _extension(filename) or ".wav"
    temp_path = ""
    req_profile = profile or "FAST"
    try:
        with tempfile.NamedTemporaryFile(suffix=ext, delete=False) as handle:
            handle.write(data)
            temp_path = handle.name

        try:
            text, detected_lang, eff_profile, model_name = await asyncio.to_thread(
                _transcribe_sync, temp_path, language, req_profile
            )
        except STTError:
            raise
        except Exception as exc:
            logger.exception("Whisper transcription internal error")
            raise STTError(422, "Speech could not be transcribed") from exc
    finally:
        if temp_path:
            try:
                os.remove(temp_path)
            except OSError:
                pass

    if not text:
        if language == "ja":
            raise STTError(422, "No Japanese speech was recognized")
        raise STTError(422, "No speech was recognized")

    return text, duration, detected_lang, eff_profile, model_name
