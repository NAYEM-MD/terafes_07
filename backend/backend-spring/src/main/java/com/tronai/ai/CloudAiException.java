package com.tronai.ai;

/**
 * Cloud provider failure that is safe to return to the client.
 * The message must never contain an API key or user content.
 */
public class CloudAiException extends AiProviderException {

    public CloudAiException(String message) {
        super(message);
    }
}
