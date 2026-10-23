package com.tronai.ai;

import com.tronai.ai.model.ModelProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tries the local provider first. Cloud is used only when fallback is enabled and local is unavailable.
 * The logical FAST / QUALITY profile is never rewritten.
 */
public class FallbackAiProvider implements AiProvider {

    public static final String LOCAL_UNAVAILABLE_DETAIL =
            "Local AI is unavailable and cloud fallback is disabled.";

    private static final Logger log = LoggerFactory.getLogger(FallbackAiProvider.class);

    private final AiProvider localProvider;
    private final AiProvider cloudProvider;
    private final CloudAiProperties cloudAiProperties;
    private final ModelProperties modelProperties;

    public FallbackAiProvider(AiProvider localProvider,
                              AiProvider cloudProvider,
                              CloudAiProperties cloudAiProperties,
                              ModelProperties modelProperties) {
        this.localProvider = localProvider;
        this.cloudProvider = cloudProvider;
        this.cloudAiProperties = cloudAiProperties != null ? cloudAiProperties : new CloudAiProperties();
        this.modelProperties = modelProperties != null ? modelProperties : new ModelProperties();
    }

    @Override
    public ChatGenerationResult generate(ChatGenerationRequest request) {
        String logicalProfile = resolveLogicalProfile(request);
        try {
            ChatGenerationResult result = localProvider.generate(request);
            return stamp(result, logicalProfile, ProviderType.LOCAL, request != null ? request.getModel() : null);
        } catch (AiProviderUnavailableException ex) {
            return fallback(request, logicalProfile);
        }
    }

    @Override
    public boolean isAvailable() {
        return localProvider != null && localProvider.isAvailable();
    }

    @Override
    public String getProviderName() {
        return localProvider != null ? localProvider.getProviderName() : "ollama";
    }

    private ChatGenerationResult fallback(ChatGenerationRequest request, String logicalProfile) {
        if (!cloudAiProperties.isEnabled()) {
            GenerationTraceHolder.clear();
            log.warn("Local AI is unavailable and cloud fallback is disabled");
            throw new AiProviderUnavailableException(LOCAL_UNAVAILABLE_DETAIL);
        }
        if (cloudProvider == null || !cloudAiProperties.isConfigured()) {
            GenerationTraceHolder.clear();
            log.warn("Local AI is unavailable and cloud fallback is not configured");
            throw new CloudAiException("Cloud AI provider is not configured.");
        }

        String cloudModel = cloudAiProperties.modelForProfile(logicalProfile);
        log.info("Local AI unavailable. Using cloud fallback for profile {} provider {}",
                logicalProfile, cloudAiProperties.getProvider());

        ChatGenerationRequest cloudRequest = copyForCloud(request, cloudModel);
        try {
            ChatGenerationResult result = cloudProvider.generate(cloudRequest);
            return stamp(result, logicalProfile, ProviderType.CLOUD, cloudModel);
        } catch (AiProviderException ex) {
            GenerationTraceHolder.clear();
            log.warn("Cloud AI provider failed for profile {}", logicalProfile);
            if (ex instanceof CloudAiException cloudEx) {
                throw cloudEx;
            }
            throw new CloudAiException("Cloud AI provider failed.");
        }
    }

    private ChatGenerationResult stamp(ChatGenerationResult result,
                                       String logicalProfile,
                                       ProviderType providerType,
                                       String actualModelId) {
        ChatGenerationResult stamped = result != null ? result : new ChatGenerationResult();
        stamped.setLogicalProfile(logicalProfile);
        stamped.setProviderType(providerType);
        stamped.setActualModelId(actualModelId);
        GenerationTraceHolder.set(new GenerationTrace(logicalProfile, providerType, actualModelId));
        return stamped;
    }

    private String resolveLogicalProfile(ChatGenerationRequest request) {
        if (request != null && request.getLogicalProfile() != null && !request.getLogicalProfile().isBlank()) {
            String value = request.getLogicalProfile().trim().toUpperCase();
            if ("QUALITY".equals(value) || "FAST".equals(value)) {
                return value;
            }
        }
        if (request != null && request.getModel() != null
                && request.getModel().trim().equalsIgnoreCase(modelProperties.getQualityModel())) {
            return "QUALITY";
        }
        return "FAST";
    }

    private static ChatGenerationRequest copyForCloud(ChatGenerationRequest request, String cloudModel) {
        if (request == null) {
            return ChatGenerationRequest.builder().model(cloudModel).build();
        }
        return ChatGenerationRequest.builder()
                .model(cloudModel)
                .messages(request.getMessages())
                .temperature(request.getTemperature())
                .numPredict(request.getNumPredict())
                .jsonMode(request.isJsonMode())
                .logicalProfile(request.getLogicalProfile())
                .build();
    }
}
