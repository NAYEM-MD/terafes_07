package com.tronai.ai;

/**
 * Internal record of one successful generation.
 * logicalProfile stays FAST or QUALITY. providerType may be LOCAL or CLOUD.
 */
public record GenerationTrace(String logicalProfile, ProviderType providerType, String actualModelId) {
}
