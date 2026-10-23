package com.tronai.cv;

import org.springframework.http.HttpStatus;

/**
 * Exception for CV upload and processing errors with an associated HTTP status.
 */
public class CvException extends RuntimeException {

    private final HttpStatus status;

    public CvException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
