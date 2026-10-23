package com.tronai.ai.gateway;

public enum GatewayProviderId {
    OLLAMA,
    GROQ,
    OPENROUTER,
    MISTRAL,
    CLOUDFLARE_WORKERS_AI,
    GEMINI;

    public String wireName() {
        return name().toLowerCase();
    }
}
