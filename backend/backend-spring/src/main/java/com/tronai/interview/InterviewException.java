package com.tronai.interview;

import org.springframework.http.HttpStatus;

/**
 * Domain exception for interview operations.
 * Maps to FastAPI-compatible {"detail": "..."} error response with custom HttpStatus.
 */
public class InterviewException extends RuntimeException {

    private final HttpStatus status;

    public InterviewException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
