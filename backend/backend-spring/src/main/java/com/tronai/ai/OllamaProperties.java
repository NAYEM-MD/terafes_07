package com.tronai.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {
    private String baseUrl = "http://127.0.0.1:11434";
    private String model = "qwen3:8b";
    private int timeoutSeconds = 60;
    private int numCtx = 4096;
}
