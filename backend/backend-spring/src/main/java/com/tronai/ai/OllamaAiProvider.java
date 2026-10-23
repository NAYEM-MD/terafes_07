package com.tronai.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Ollama implementation of the AiProvider interface.
 * Primary so health and single-provider injection stay on the local model.
 */
@Component
@Primary
public class OllamaAiProvider implements AiProvider {

    private final OllamaClient ollamaClient;

    public OllamaAiProvider(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @Override
    public ChatGenerationResult generate(ChatGenerationRequest request) {
        return ollamaClient.chat(request);
    }

    @Override
    public boolean isAvailable() {
        return ollamaClient.checkHealth();
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }
}
