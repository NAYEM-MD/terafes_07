package com.tronai.desktopagent;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DesktopAgentException extends ResponseStatusException {

    private final String errorCode;

    public DesktopAgentException(HttpStatus status, String reason) {
        this(status, reason, null);
    }

    public DesktopAgentException(HttpStatus status, String reason, String errorCode) {
        super(status, reason);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
