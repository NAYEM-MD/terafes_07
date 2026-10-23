package com.tronai.ai.gateway.provider;

public class GatewayProviderException extends RuntimeException {

    public static final String INVALID_CREDENTIAL = "INVALID_CREDENTIAL";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String PAYMENT_REQUIRED = "PAYMENT_REQUIRED";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UPSTREAM_ERROR = "UPSTREAM_ERROR";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String UNAVAILABLE = "UNAVAILABLE";

    private final String category;
    private final int httpStatus;

    public GatewayProviderException(String category, int httpStatus, String message) {
        super(message);
        this.category = category;
        this.httpStatus = httpStatus;
    }

    public GatewayProviderException(String category, int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
        this.httpStatus = httpStatus;
    }

    public String getCategory() {
        return category;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
