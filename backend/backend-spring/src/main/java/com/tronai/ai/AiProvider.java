package com.tronai.ai;

/**
 * AI Provider abstraction interface.
 * Decouples ChatService from specific backends (Ollama, vLLM, etc.).
 */
public interface AiProvider {

    /**
     * Generate chat completion given request.
     */
    ChatGenerationResult generate(ChatGenerationRequest request);

    /**
     * Check if provider is reachable and active.
     */
    boolean isAvailable();

    /**
     * Provider identifier (e.g. "ollama", "vllm").
     */
    String getProviderName();
}
