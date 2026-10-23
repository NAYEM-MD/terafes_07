package com.tronai.ai.model;

/**
 * Thrown when a requested model is registered but currently disabled.
 */
public class ModelDisabledException extends RuntimeException {
    public ModelDisabledException(String message) {
        super(message);
    }
}
