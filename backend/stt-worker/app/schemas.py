from __future__ import annotations

from typing import Optional
from pydantic import BaseModel, Field


class TranscriptionResponse(BaseModel):
    text: str = Field(..., description="Recognized speech transcript")
    duration: float = Field(..., description="Audio duration in seconds")
    language: str = Field(..., description="Detected or specified language code")
    profile: Optional[str] = Field(default="FAST", description="STT profile used")
    model_used: Optional[str] = Field(default="base", description="Actual Whisper model used")


class HealthResponse(BaseModel):
    status: str = Field(default="ok")
    service: str = Field(default="tron-ai-stt-worker")
    model: str
    device: str
    compute_type: str
    profiles: dict[str, str] = Field(default_factory=dict)


class ErrorDetail(BaseModel):
    detail: str
