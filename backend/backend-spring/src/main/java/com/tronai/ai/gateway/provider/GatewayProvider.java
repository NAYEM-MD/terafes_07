package com.tronai.ai.gateway.provider;

import com.tronai.ai.gateway.AiGenerationResult;
import com.tronai.ai.gateway.AiRequestContext;
import com.tronai.ai.gateway.GatewayProviderId;
import com.tronai.ai.gateway.ProviderHealthStatus;

import java.util.List;

public interface GatewayProvider {

    GatewayProviderId id();

    boolean isConfigured();

    ProviderHealthStatus health();

    default String healthDetail() {
        return "";
    }

    AiGenerationResult generate(AiRequestContext context, String modelId);

    List<String> knownModels();
}
