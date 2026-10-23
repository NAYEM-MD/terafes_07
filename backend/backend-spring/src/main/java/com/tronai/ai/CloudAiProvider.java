package com.tronai.ai;

/**
 * Cloud AI provider stub.
 * No vendor HTTP call is made. A real transport is added only when a provider contract exists.
 * This class is not a Spring bean, so it does not compete with the local Ollama provider.
 */
public class CloudAiProvider implements AiProvider {

    private final CloudAiProperties properties;

    public CloudAiProvider(CloudAiProperties properties) {
        this.properties = properties != null ? properties : new CloudAiProperties();
    }

    @Override
    public ChatGenerationResult generate(ChatGenerationRequest request) {
        if (!properties.isEnabled()) {
            throw new CloudAiException("Cloud AI fallback is disabled.");
        }
        if (!properties.isConfigured()) {
            throw new CloudAiException("Cloud AI provider is not configured.");
        }
        throw new CloudAiException("Cloud AI provider is not connected.");
    }

    @Override
    public boolean isAvailable() {
        return properties.isConfigured();
    }

    @Override
    public String getProviderName() {
        return "cloud";
    }
}
