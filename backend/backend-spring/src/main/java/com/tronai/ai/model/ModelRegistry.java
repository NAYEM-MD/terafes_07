package com.tronai.ai.model;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of supported model profiles.
 */
@Component
public class ModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelRegistry.class);

    private final ModelProperties modelProperties;
    private final Map<String, ModelProfile> registry = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public ModelRegistry(ModelProperties modelProperties) {
        this.modelProperties = modelProperties != null ? modelProperties : new ModelProperties();
    }

    public ModelRegistry() {
        this(new ModelProperties());
    }

    @PostConstruct
    public void init() {
        String fastModelName = modelProperties.getFastModel();
        String qualityModelName = modelProperties.getQualityModel();

        // 1. FAST Profile Model: tron-fast
        register(ModelProfile.builder()
                .id("tron-fast")
                .displayName("TRON Fast (" + fastModelName + ")")
                .provider(ModelProviderType.OLLAMA)
                .providerModelName(fastModelName)
                .capability(ModelCapability.CHAT)
                .capability(ModelCapability.CODING)
                .capability(ModelCapability.INTERVIEW)
                .capability(ModelCapability.MULTILINGUAL)
                .enabled(true)
                .contextWindow(4096)
                .maxOutputTokens(1536)
                .build());

        // 2. QUALITY Profile Model: tron-quality
        register(ModelProfile.builder()
                .id("tron-quality")
                .displayName("TRON Quality (" + qualityModelName + ")")
                .provider(ModelProviderType.OLLAMA)
                .providerModelName(qualityModelName)
                .capability(ModelCapability.CHAT)
                .capability(ModelCapability.CODING)
                .capability(ModelCapability.INTERVIEW)
                .capability(ModelCapability.MULTILINGUAL)
                .enabled(true)
                .contextWindow(8192)
                .maxOutputTokens(2048)
                .build());

        // 3. Backward Compatibility Alias: qwen3-8b
        register(ModelProfile.builder()
                .id("qwen3-8b")
                .displayName("Qwen3 8B")
                .provider(ModelProviderType.OLLAMA)
                .providerModelName(fastModelName)
                .capability(ModelCapability.CHAT)
                .capability(ModelCapability.CODING)
                .capability(ModelCapability.INTERVIEW)
                .capability(ModelCapability.MULTILINGUAL)
                .enabled(true)
                .contextWindow(4096)
                .maxOutputTokens(1536)
                .build());
    }

    public void register(ModelProfile profile) {
        if (profile == null || profile.getId() == null || profile.getId().isBlank()) {
            throw new IllegalArgumentException("Model profile and ID must not be null or blank");
        }
        registry.put(profile.getId().toLowerCase(), profile);
        log.info("Registered model profile: {} -> {} ({})", profile.getId(), profile.getDisplayName(), profile.getProvider());
    }

    public Optional<ModelProfile> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(registry.get(id.trim().toLowerCase()));
    }

    public List<ModelProfile> findAll() {
        return new ArrayList<>(registry.values());
    }

    public List<ModelProfile> findEnabled() {
        return registry.values().stream()
                .filter(ModelProfile::isEnabled)
                .toList();
    }

    public boolean existsById(String id) {
        if (id == null) {
            return false;
        }
        return registry.containsKey(id.trim().toLowerCase());
    }
}
