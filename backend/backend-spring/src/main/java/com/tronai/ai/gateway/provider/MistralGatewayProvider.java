package com.tronai.ai.gateway.provider;

import com.tronai.ai.gateway.AiGatewayProperties;
import com.tronai.ai.gateway.AiGenerationResult;
import com.tronai.ai.gateway.AiRequestContext;
import com.tronai.ai.gateway.GatewayProviderId;
import com.tronai.ai.gateway.ProviderHealthStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class MistralGatewayProvider implements GatewayProvider {

    private static final String BASE_URL = "https://api.mistral.ai/v1";

    private final AiGatewayProperties properties;
    private final OpenAiCompatibleClient client;

    public MistralGatewayProvider(AiGatewayProperties properties, OpenAiCompatibleClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public GatewayProviderId id() {
        return GatewayProviderId.MISTRAL;
    }

    @Override
    public boolean isConfigured() {
        return hasText(properties.getMistralApiKey());
    }

    @Override
    public ProviderHealthStatus health() {
        return isConfigured() ? ProviderHealthStatus.CONFIGURED : ProviderHealthStatus.DISABLED;
    }

    @Override
    public List<String> knownModels() {
        return Stream.of(properties.getMistralModelSmart(), properties.getMistralModelUltra())
                .filter(MistralGatewayProvider::hasText)
                .distinct()
                .toList();
    }

    @Override
    public AiGenerationResult generate(AiRequestContext context, String modelId) {
        if (!isConfigured()) {
            throw new GatewayProviderException(GatewayProviderException.UNAVAILABLE, 503, "Mistral is not configured");
        }
        String model = hasText(modelId) ? modelId : properties.getMistralModelSmart();
        AiGenerationResult result = client.generate(
                BASE_URL,
                properties.getMistralApiKey().trim(),
                model,
                context.getMessages(),
                context.getTemperature(),
                context.getMaxTokens(),
                context.isJsonMode(),
                Map.of(),
                properties.getRequestTimeoutSeconds());
        enrich(result, context, model);
        return result;
    }

    private void enrich(AiGenerationResult result, AiRequestContext context, String model) {
        result.setProvider(GatewayProviderId.MISTRAL);
        result.setModelId(model);
        result.setMode(context.getMode());
        result.setFeature(context.getFeature());
        result.setRequestId(context.getRequestId());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
