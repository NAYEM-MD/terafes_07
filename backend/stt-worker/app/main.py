from __future__ import annotations

import logging
from typing import Optional

from fastapi import FastAPI, File, Form, Request, UploadFile
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import settings
from app.errors import STTError
from app.schemas import HealthResponse, TranscriptionResponse
from app.transcriber import transcribe

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("stt_worker")

app = FastAPI(title="TRON AI STT Worker", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(STTError)
async def stt_error_handler(_: Request, exc: STTError):
    return JSONResponse(status_code=exc.status_code, content={"detail": exc.detail})


@app.exception_handler(RequestValidationError)
async def validation_error_handler(_: Request, exc: RequestValidationError):
    errors = exc.errors()
    msg = errors[0].get("msg", "Invalid request") if errors else "Invalid request"
    return JSONResponse(status_code=422, content={"detail": msg})


@app.exception_handler(Exception)
async def generic_error_handler(_: Request, exc: Exception):
    logger.exception("Unexpected error in STT worker: %s", exc)
    return JSONResponse(status_code=500, content={"detail": "Internal STT worker error"})


@app.get("/health", response_model=HealthResponse)
async def health_check() -> HealthResponse:
    return HealthResponse(
        status="ok",
        service="tron-ai-stt-worker",
        model=settings.whisper_fast_model,
        device=settings.whisper_device,
        compute_type=settings.whisper_fast_compute_type,
        profiles={
            "FAST": settings.whisper_fast_model,
            "QUALITY": settings.whisper_quality_model,
        },
    )


@app.post("/v1/transcribe", response_model=TranscriptionResponse)
async def transcribe_audio_endpoint(
    file: UploadFile = File(...),
    language: Optional[str] = Form(default="ja"),
    profile: Optional[str] = Form(default="FAST"),
) -> TranscriptionResponse:
    try:
        data = await file.read(settings.max_audio_bytes + 1)
        filename = file.filename or ""
        content_type = file.content_type or ""
        text, duration, detected_language, eff_profile, model_name = await transcribe(
            filename=filename,
            content_type=content_type,
            data=data,
            language=language,
            profile=profile,
        )
        return TranscriptionResponse(
            text=text,
            duration=duration,
            language=detected_language,
            profile=eff_profile,
            model_used=model_name,
        )
    finally:
        await file.close()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host=settings.host, port=settings.port, reload=False)
