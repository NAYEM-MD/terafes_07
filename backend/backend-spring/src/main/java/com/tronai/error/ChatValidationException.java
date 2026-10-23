package com.tronai.error;

public class ChatValidationException extends RuntimeException {
    public ChatValidationException(String message) {
        super(message);
    }
}
