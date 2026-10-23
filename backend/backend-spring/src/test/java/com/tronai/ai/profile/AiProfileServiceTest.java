package com.tronai.ai.profile;

import com.tronai.ai.AiProvider;
import com.tronai.ai.model.ModelManager;
import com.tronai.ai.model.ModelProfile;
import com.tronai.ai.model.ModelProperties;
import com.tronai.ai.model.ModelRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AiProfileServiceTest {

    private ModelProperties modelProperties;
    private ModelRegistry modelRegistry;
    private ModelManager modelManager;
    private AiProvider aiProvider;
    private AiProfileService aiProfileService;

    @BeforeEach
    void setUp() {
        modelProperties = new ModelProperties();
        modelProperties.setFastModel("qwen3:8b");
        modelProperties.setQualityModel("gemma3:12b");
        modelProperties.setDefaultProfile("FAST");

        modelRegistry = new ModelRegistry(modelProperties);
        modelRegistry.init();

        aiProvider = mock(AiProvider.class);
        when(aiProvider.isAvailable()).thenReturn(true);

        modelManager = mock(ModelManager.class);
        when(modelManager.getProvider(any())).thenReturn(aiProvider);

        aiProfileService = new AiProfileService(modelRegistry, modelProperties, modelManager);
    }

    @Test
    @DisplayName("FAST profile resolves to tron-fast and configured qwen3:8b provider model")
    void testFastProfileResolution() {
        ModelProfile profile = aiProfileService.resolveModel(AiProfile.FAST);
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo("tron-fast");
        assertThat(profile.getProviderModelName()).isEqualTo("qwen3:8b");
    }

    @Test
    @DisplayName("QUALITY profile resolves to tron-quality and configured gemma3:12b provider model")
    void testQualityProfileResolution() {
        ModelProfile profile = aiProfileService.resolveModel(AiProfile.QUALITY);
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo("tron-quality");
        assertThat(profile.getProviderModelName()).isEqualTo("gemma3:12b");
    }

    @Test
    @DisplayName("Null or empty profile safely defaults to FAST, unknown profile string is rejected")
    void testNullProfileDefaultsToFast() {
        assertThat(aiProfileService.resolveModel((AiProfile) null).getId()).isEqualTo("tron-fast");
        assertThat(aiProfileService.resolveModel((String) null).getId()).isEqualTo("tron-fast");
        assertThat(aiProfileService.resolveModel("").getId()).isEqualTo("tron-fast");

        assertThatThrownBy(() -> aiProfileService.resolveModel("UNKNOWN_PROFILE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown AI profile: 'UNKNOWN_PROFILE'");
    }

    @Test
    @DisplayName("GET /api/v1/ai-profiles response structure returns Fast and Quality definitions")
    void testGetProfilesResponse() {
        AiProfilesResponse response = aiProfileService.getAvailableProfiles();
        assertThat(response.getDefaultProfile()).isEqualTo("FAST");
        assertThat(response.getProfiles()).hasSizeGreaterThanOrEqualTo(2);

        AiProfileDefinition fastDef = response.getProfiles().stream()
                .filter(p -> "FAST".equals(p.getId()))
                .findFirst().orElseThrow();
        assertThat(fastDef.isAvailable()).isTrue();
        assertThat(fastDef.getModelDisplayName()).contains("qwen3:8b");

        AiProfileDefinition qualityDef = response.getProfiles().stream()
                .filter(p -> "QUALITY".equals(p.getId()))
                .findFirst().orElseThrow();
        assertThat(qualityDef.getModelDisplayName()).contains("gemma3:12b");
        assertThat(qualityDef.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("When Quality provider is unavailable, resolveModel throws explicit 503 Service Unavailable, no silent downgrade")
    void testQualityModelFailureThrows503WithoutDowngrade() {
        when(aiProvider.isAvailable()).thenReturn(false);

        assertThatThrownBy(() -> aiProfileService.resolveModel(AiProfile.QUALITY))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Quality AI mode is temporarily unavailable");
    }
}
