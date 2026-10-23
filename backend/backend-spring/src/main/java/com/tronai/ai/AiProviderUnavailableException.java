package com.tronai.ai;

public class AiProviderUnavailableException extends AiProviderException {
    public AiProviderUnavailableException(String message) {
        super(message);
    }

    public AiProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
