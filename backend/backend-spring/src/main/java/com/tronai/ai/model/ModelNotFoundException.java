package com.tronai.ai.model;

/**
 * Thrown when a requested model ID is not registered.
 */
public class ModelNotFoundException extends RuntimeException {
    public ModelNotFoundException(String message) {
        super(message);
    }
}
