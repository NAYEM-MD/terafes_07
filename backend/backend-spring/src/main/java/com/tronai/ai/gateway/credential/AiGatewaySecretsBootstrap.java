package com.tronai.ai.gateway.credential;

import com.tronai.ai.gateway.AiGatewayProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiGatewaySecretsBootstrap {

    private final AiGatewayProperties properties;

    public AiGatewaySecretsBootstrap(AiGatewayProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void mergeLocalSecrets() {
        Map<String, String> secrets = LocalSecretsLoader.load(LocalSecretsLoader.defaultPath());
        if (secrets.isEmpty()) {
            return;
        }
        mergeIfBlank(secrets, "GROQ_API_KEY", properties::getGroqApiKey, properties::setGroqApiKey);
        mergeIfBlank(secrets, "OPENROUTER_API_KEY_1", properties::getOpenrouterApiKey1, properties::setOpenrouterApiKey1);
        mergeIfBlank(secrets, "OPENROUTER_API_KEY_2", properties::getOpenrouterApiKey2, properties::setOpenrouterApiKey2);
        mergeIfBlank(secrets, "MISTRAL_API_KEY", properties::getMistralApiKey, properties::setMistralApiKey);
        mergeIfBlank(secrets, "CLOUDFLARE_API_TOKEN", properties::getCloudflareApiToken, properties::setCloudflareApiToken);
        mergeIfBlank(secrets, "CLOUDFLARE_ACCOUNT_ID", properties::getCloudflareAccountId, properties::setCloudflareAccountId);
        mergeIfBlank(secrets, "GEMINI_API_KEY_1", properties::getGeminiApiKey1, properties::setGeminiApiKey1);
        mergeIfBlank(secrets, "GEMINI_API_KEY_2", properties::getGeminiApiKey2, properties::setGeminiApiKey2);
        mergeIfBlank(secrets, "GEMINI_API_KEY_3", properties::getGeminiApiKey3, properties::setGeminiApiKey3);
        mergeIfBlank(secrets, "GEMINI_API_KEY_4", properties::getGeminiApiKey4, properties::setGeminiApiKey4);
    }

    private void mergeIfBlank(
            Map<String, String> secrets,
            String key,
            java.util.function.Supplier<String> getter,
            java.util.function.Consumer<String> setter) {
        String current = getter.get();
        if (current != null && !current.isBlank()) {
            return;
        }
        String value = secrets.get(key);
        if (value != null && !value.isBlank()) {
            setter.accept(value.trim());
        }
    }
}
