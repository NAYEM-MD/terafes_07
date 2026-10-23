package com.tronai.ai.model;

import com.tronai.ai.AiProvider;
import com.tronai.ai.AiProviderUnavailableException;
import com.tronai.ai.CloudAiProperties;
import com.tronai.ai.CloudAiProvider;
import com.tronai.ai.FallbackAiProvider;
import com.tronai.ai.model.dto.ModelItemDto;
import com.tronai.ai.model.dto.ModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Model Manager orchestrating model resolution, validation, provider mapping,
 * and available model discovery.
 */
@Service
public class ModelManager {

    private static final Logger log = LoggerFactory.getLogger(ModelManager.class);

    private final ModelRegistry modelRegistry;
    private final ModelProperties modelProperties;
    private final List<AiProvider> aiProviders;
    private final CloudAiProperties cloudAiProperties;

    public ModelManager(ModelRegistry modelRegistry, ModelProperties modelProperties, List<AiProvider> aiProviders) {
        this(modelRegistry, modelProperties, aiProviders, new CloudAiProperties());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ModelManager(ModelRegistry modelRegistry,
                        ModelProperties modelProperties,
                        List<AiProvider> aiProviders,
                        CloudAiProperties cloudAiProperties) {
        this.modelRegistry = modelRegistry;
        this.modelProperties = modelProperties;
        this.aiProviders = aiProviders != null ? aiProviders : List.of();
        this.cloudAiProperties = cloudAiProperties != null ? cloudAiProperties : new CloudAiProperties();
    }

    /**
     * Resolves the default configured model profile.
     */
    public ModelProfile getDefaultModel() {
        String defaultId = modelProperties.getDefaultModel();
        return modelRegistry.findById(defaultId)
                .filter(ModelProfile::isEnabled)
                .orElseGet(() -> {
                    List<ModelProfile> enabled = modelRegistry.findEnabled();
                    if (!enabled.isEmpty()) {
                        log.warn("Configured default model '{}' not found or disabled. Falling back to '{}'",
                                defaultId, enabled.get(0).getId());
                        return enabled.get(0);
                    }
                    throw new ModelNotFoundException("No enabled models configured in registry");
                });
    }

    /**
     * Resolves a model profile by logical ID. If null or blank, returns default model.
     * Validates that the model exists and is enabled.
     */
    public ModelProfile resolveModel(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return getDefaultModel();
        }

        ModelProfile profile = modelRegistry.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException("Model not found: " + modelId));

        if (!profile.isEnabled()) {
            throw new ModelDisabledException("Model is disabled: " + modelId);
        }

        return profile;
    }

    public ModelProfile getModel(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return null;
        }
        return modelRegistry.findById(modelId).orElse(null);
    }

    /**
     * Resolves the appropriate AiProvider implementation for a given provider type.
     */
    public AiProvider getProvider(ModelProviderType providerType) {
        if (providerType == null) {
            throw new IllegalArgumentException("ModelProviderType must not be null");
        }

        String targetName = providerType.name().toLowerCase();
        for (AiProvider provider : aiProviders) {
            if (targetName.equalsIgnoreCase(provider.getProviderName())) {
                if (providerType == ModelProviderType.OLLAMA) {
                    return new FallbackAiProvider(
                            provider,
                            new CloudAiProvider(cloudAiProperties),
                            cloudAiProperties,
                            modelProperties
                    );
                }
                return provider;
            }
        }

        throw new AiProviderUnavailableException("No active AI provider configured for provider type: " + providerType);
    }

    /**
     * Constructs the read-only models response listing all registered models
     * along with dynamic availability status.
     */
    public ModelResponse getModelsResponse() {
        String defaultModelId = getDefaultModel().getId();
        List<ModelItemDto> items = new ArrayList<>();

        for (ModelProfile profile : modelRegistry.findAll()) {
            boolean available = false;
            try {
                AiProvider provider = getProvider(profile.getProvider());
                available = provider.isAvailable();
            } catch (Exception e) {
                log.debug("Provider availability check failed for {}: {}", profile.getId(), e.getMessage());
            }

            List<String> capabilityNames = profile.getCapabilities().stream()
                    .map(Enum::name)
                    .sorted()
                    .toList();

            items.add(ModelItemDto.builder()
                    .id(profile.getId())
                    .displayName(profile.getDisplayName())
                    .provider(profile.getProvider().name())
                    .capabilities(capabilityNames)
                    .enabled(profile.isEnabled())
                    .available(available)
                    .build());
        }

        return ModelResponse.builder()
                .defaultModel(defaultModelId)
                .models(items)
                .build();
    }
}
