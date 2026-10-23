import io
import wave
from unittest.mock import patch, MagicMock

import pytest
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def _make_dummy_wav(duration_seconds: float = 1.0, sample_rate: int = 16000) -> bytes:
    buf = io.BytesIO()
    with wave.open(buf, "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        num_frames = int(duration_seconds * sample_rate)
        wav.writeframes(b"\x00\x00" * num_frames)
    return buf.getvalue()


def test_empty_audio_rejected():
    files = {"file": ("test.wav", b"", "audio/wav")}
    response = client.post("/v1/transcribe", files=files)
    assert response.status_code == 400
    assert "empty" in response.json()["detail"].lower()


def test_unsupported_format_rejected():
    files = {"file": ("test.exe", b"MZDummyBinaryData", "application/octet-stream")}
    response = client.post("/v1/transcribe", files=files)
    assert response.status_code == 415


def test_corrupted_wav_rejected():
    files = {"file": ("corrupt.wav", b"RIFF\x00\x00\x00\x00WAVEbadcontent", "audio/wav")}
    response = client.post("/v1/transcribe", files=files)
    assert response.status_code == 400
    assert "invalid" in response.json()["detail"].lower()


def test_transcribe_success_mocked():
    wav_bytes = _make_dummy_wav(1.5)
    files = {"file": ("test.wav", wav_bytes, "audio/wav")}

    with patch("app.transcriber._transcribe_sync") as mock_transcribe:
        mock_transcribe.return_value = ("こんにちは、世界", "ja", "FAST", "base")
        response = client.post("/v1/transcribe", files=files, data={"language": "ja", "profile": "FAST"})
        assert response.status_code == 200
        data = response.json()
        assert data["text"] == "こんにちは、世界"
        assert data["language"] == "ja"
        assert data["duration"] == 1.5
        assert data["profile"] == "FAST"
        assert data["model_used"] == "base"


def test_transcribe_quality_profile_mocked():
    wav_bytes = _make_dummy_wav(1.5)
    files = {"file": ("test.wav", wav_bytes, "audio/wav")}

    with patch("app.transcriber._transcribe_sync") as mock_transcribe:
        mock_transcribe.return_value = ("高度な推論面接回答です", "ja", "QUALITY", "base")
        response = client.post("/v1/transcribe", files=files, data={"language": "ja", "profile": "QUALITY"})
        assert response.status_code == 200
        data = response.json()
        assert data["text"] == "高度な推論面接回答です"
        assert data["language"] == "ja"
        assert data["profile"] == "QUALITY"


def test_no_speech_recognized_422():
    wav_bytes = _make_dummy_wav(1.0)
    files = {"file": ("test.wav", wav_bytes, "audio/wav")}

    with patch("app.transcriber._transcribe_sync") as mock_transcribe:
        mock_transcribe.return_value = ("", "ja", "FAST", "base")
        response = client.post("/v1/transcribe", files=files, data={"language": "ja"})
        assert response.status_code == 422
        assert "no japanese speech was recognized" in response.json()["detail"].lower()


def test_transcribe_english_mocked():
    wav_bytes = _make_dummy_wav(2.0)
    files = {"file": ("test.wav", wav_bytes, "audio/wav")}

    with patch("app.transcriber._transcribe_sync") as mock_transcribe:
        mock_transcribe.return_value = ("Hello world, this is a test.", "en", "FAST", "base")
        response = client.post("/v1/transcribe", files=files, data={"language": "en"})
        assert response.status_code == 200
        data = response.json()
        assert data["text"] == "Hello world, this is a test."
        assert data["language"] == "en"
        assert data["duration"] == 2.0


def test_oversized_audio_rejected():
    with patch("app.config.settings.max_audio_bytes", 100):
        files = {"file": ("test.wav", b"A" * 200, "audio/wav")}
        response = client.post("/v1/transcribe", files=files)
        assert response.status_code == 413
