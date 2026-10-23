package com.tronai.voice;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for voice, STT, and TTS operations.
 */
public class VoiceException extends RuntimeException {

    private final HttpStatus status;

    public VoiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public VoiceException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
