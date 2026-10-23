package com.tronai.ai.gateway;

/**
 * User-facing intelligence modes for the shared AI gateway.
 * FAST and QUALITY remain valid for existing clients.
 */
public enum AiMode {
    FAST,
    SMART,
    QUALITY,
    ULTRA,
    AUTO;

    public static AiMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        String clean = value.trim().toUpperCase();
        for (AiMode mode : values()) {
            if (mode.name().equals(clean)) {
                return mode;
            }
        }
        throw new IllegalArgumentException(
                "Unknown AI mode: '" + value + "'. Allowed: FAST, SMART, QUALITY, ULTRA, AUTO");
    }

    /** QUALITY is treated as SMART for routing while remaining API-compatible. */
    public AiMode routingMode() {
        return this == QUALITY ? SMART : this;
    }

    public boolean consumesUltraCredits() {
        return this == ULTRA;
    }
}
