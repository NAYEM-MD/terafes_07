package com.tronai.ai;

public class AiProviderTimeoutException extends AiProviderException {
    public AiProviderTimeoutException(String message) {
        super(message);
    }

    public AiProviderTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
