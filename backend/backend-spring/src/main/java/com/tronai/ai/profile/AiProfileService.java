package com.tronai.ai.profile;

import com.tronai.ai.AiProvider;
import com.tronai.ai.CloudAiProperties;
import com.tronai.ai.model.ModelManager;
import com.tronai.ai.model.ModelProfile;
import com.tronai.ai.model.ModelProperties;
import com.tronai.ai.model.ModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Universal AI Profile Router and Service.
 * Central authority for mapping logical AI Profiles (FAST, QUALITY) to provider models,
 * validating profile availability, and enforcing consistent model routing across all TRON features.
 */
@Service
public class AiProfileService {

    private static final Logger log = LoggerFactory.getLogger(AiProfileService.class);

    private final ModelRegistry modelRegistry;
    private final ModelProperties modelProperties;
    private final ModelManager modelManager;
    private CloudAiProperties cloudAiProperties = new CloudAiProperties();

    public AiProfileService(ModelRegistry modelRegistry, ModelProperties modelProperties, ModelManager modelManager) {
        this.modelRegistry = modelRegistry;
        this.modelProperties = modelProperties;
        this.modelManager = modelManager;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setCloudAiProperties(CloudAiProperties cloudAiProperties) {
        if (cloudAiProperties != null) {
            this.cloudAiProperties = cloudAiProperties;
        }
    }

    /**
     * Resolves the ModelProfile corresponding to the requested logical AiProfile.
     * Enforces that a locked QUALITY session fails explicitly rather than silently downgrading to FAST.
     */
    public ModelProfile resolveModel(AiProfile profile) {
        AiProfile targetProfile = profile != null ? profile : AiProfile.FAST;
        String modelId = targetProfile.getModelId();

        ModelProfile modelProfile = modelRegistry.findById(modelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "AI model profile '" + modelId + "' is not registered."));

        if (!modelProfile.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    targetProfile.getDisplayName() + " AI mode is currently disabled.");
        }

        // Verify provider availability for QUALITY profile specifically
        if (targetProfile == AiProfile.QUALITY) {
            try {
                AiProvider provider = modelManager.getProvider(modelProfile.getProvider());
                if (!provider.isAvailable() && !cloudFallbackReady()) {
                    log.warn("Quality model '{}' provider is unavailable", modelProfile.getProviderModelName());
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Quality AI mode is temporarily unavailable.");
                }
            } catch (ResponseStatusException rse) {
                throw rse;
            } catch (Exception e) {
                if (!cloudFallbackReady()) {
                    log.warn("Quality model check failed: {}", e.getMessage());
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Quality AI mode is temporarily unavailable.");
                }
            }
        }

        return modelProfile;
    }

    /**
     * Overload to resolve by profile name string (defaults to FAST if null/blank).
     */
    public ModelProfile resolveModel(String profileString) {
        AiProfile profile = parseProfile(profileString);
        return resolveModel(profile);
    }

    /**
     * Parses a string into an AiProfile enum with strict validation and backward compatibility.
     * Defaults to FAST if null or empty.
     */
    public AiProfile parseProfile(String profileString) {
        return AiProfile.fromString(profileString);
    }

    /**
     * Returns the list of configured AI profiles and their dynamic availability status.
     */
    public AiProfilesResponse getAvailableProfiles() {
        List<AiProfileDefinition> definitions = new ArrayList<>();

        for (AiProfile p : AiProfile.values()) {
            ModelProfile modelProfile = modelRegistry.findById(p.getModelId()).orElse(null);
            boolean available = false;
            String reason = null;
            String modelDisplayName = modelProfile != null ? modelProfile.getProviderModelName() : "Unknown";

            if (modelProfile != null && modelProfile.isEnabled()) {
                try {
                    AiProvider provider = modelManager.getProvider(modelProfile.getProvider());
                    available = provider.isAvailable() || cloudFallbackReady();
                    if (!available) {
                        reason = "Model service is not reachable.";
                    }
                } catch (Exception e) {
                    available = false;
                    reason = "AI provider is currently unavailable.";
                }
            } else {
                reason = "Profile is not enabled.";
            }

            definitions.add(AiProfileDefinition.builder()
                    .id(p.name())
                    .displayName(p.getDisplayName())
                    .description(p.getDescription())
                    .available(available)
                    .modelDisplayName(modelDisplayName)
                    .reasonUnavailable(reason)
                    .build());
        }

        return AiProfilesResponse.builder()
                .profiles(definitions)
                .defaultProfile(modelProperties.getDefaultProfile())
                .build();
    }

    private boolean cloudFallbackReady() {
        return cloudAiProperties != null && cloudAiProperties.isConfigured();
    }
}
