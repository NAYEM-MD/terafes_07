package com.tronai.ai.model;

import com.tronai.ai.AiProvider;
import com.tronai.ai.AiProviderUnavailableException;
import com.tronai.ai.model.dto.ModelItemDto;
import com.tronai.ai.model.dto.ModelResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelManagerTest {

    private ModelRegistry modelRegistry;
    private ModelProperties modelProperties;

    @Mock
    private AiProvider ollamaProvider;

    private ModelManager modelManager;

    @BeforeEach
    void setUp() {
        modelRegistry = new ModelRegistry();
        modelRegistry.init();

        modelProperties = new ModelProperties();
        modelProperties.setDefaultModel("qwen3-8b");

        org.mockito.Mockito.lenient().when(ollamaProvider.getProviderName()).thenReturn("ollama");

        modelManager = new ModelManager(modelRegistry, modelProperties, List.of(ollamaProvider));
    }

    @Test
    @DisplayName("ModelRegistry initializes with active qwen3-8b profile")
    void registryContainsQwen38b() {
        Optional<ModelProfile> profileOpt = modelRegistry.findById("qwen3-8b");
        assertThat(profileOpt).isPresent();

        ModelProfile profile = profileOpt.get();
        assertThat(profile.getId()).isEqualTo("qwen3-8b");
        assertThat(profile.getDisplayName()).isEqualTo("Qwen3 8B");
        assertThat(profile.getProvider()).isEqualTo(ModelProviderType.OLLAMA);
        assertThat(profile.getProviderModelName()).isEqualTo("qwen3:8b");
        assertThat(profile.isEnabled()).isTrue();
        assertThat(profile.getCapabilities()).containsExactlyInAnyOrder(
                ModelCapability.CHAT,
                ModelCapability.CODING,
                ModelCapability.INTERVIEW,
                ModelCapability.MULTILINGUAL
        );
    }

    @Test
    @DisplayName("Default model resolves correctly to qwen3-8b")
    void defaultModelResolvesCorrectly() {
        ModelProfile defaultModel = modelManager.getDefaultModel();
        assertThat(defaultModel).isNotNull();
        assertThat(defaultModel.getId()).isEqualTo("qwen3-8b");
        assertThat(defaultModel.getProviderModelName()).isEqualTo("qwen3:8b");
        assertThat(defaultModel.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("resolveModel with null or blank returns default model")
    void resolveModelWithNullOrBlankReturnsDefault() {
        ModelProfile profileNull = modelManager.resolveModel(null);
        assertThat(profileNull.getId()).isEqualTo("qwen3-8b");

        ModelProfile profileBlank = modelManager.resolveModel("   ");
        assertThat(profileBlank.getId()).isEqualTo("qwen3-8b");
    }

    @Test
    @DisplayName("resolveModel with valid ID returns exact profile")
    void resolveModelWithValidIdReturnsProfile() {
        ModelProfile profile = modelManager.resolveModel("qwen3-8b");
        assertThat(profile.getId()).isEqualTo("qwen3-8b");
        assertThat(profile.getDisplayName()).isEqualTo("Qwen3 8B");
    }

    @Test
    @DisplayName("Unknown model fails safely with ModelNotFoundException")
    void unknownModelFailsSafely() {
        assertThatThrownBy(() -> modelManager.resolveModel("non-existent-model"))
                .isInstanceOf(ModelNotFoundException.class)
                .hasMessageContaining("Model not found: non-existent-model");
    }

    @Test
    @DisplayName("Disabled model cannot be selected and throws ModelDisabledException")
    void disabledModelCannotBeSelected() {
        ModelProfile disabledProfile = ModelProfile.builder()
                .id("disabled-test-model")
                .displayName("Disabled Test Model")
                .provider(ModelProviderType.OLLAMA)
                .providerModelName("test:latest")
                .enabled(false)
                .build();
        modelRegistry.register(disabledProfile);

        assertThatThrownBy(() -> modelManager.resolveModel("disabled-test-model"))
                .isInstanceOf(ModelDisabledException.class)
                .hasMessageContaining("Model is disabled: disabled-test-model");
    }

    @Test
    @DisplayName("ModelManager chooses Ollama provider based on provider type")
    void choosesOllamaProvider() {
        AiProvider provider = modelManager.getProvider(ModelProviderType.OLLAMA);
        assertThat(provider).isNotNull();
        assertThat(provider.getProviderName()).isEqualTo("ollama");
    }

    @Test
    @DisplayName("Requesting provider with no registered implementation throws AiProviderUnavailableException")
    void unconfiguredProviderThrowsException() {
        assertThatThrownBy(() -> modelManager.getProvider(ModelProviderType.VLLM))
                .isInstanceOf(AiProviderUnavailableException.class)
                .hasMessageContaining("No active AI provider configured for provider type: VLLM");
    }

    @Test
    @DisplayName("getModelsResponse returns safe configured metadata with availability")
    void getModelsResponseIncludesAvailability() {
        when(ollamaProvider.isAvailable()).thenReturn(true);

        ModelResponse response = modelManager.getModelsResponse();
        assertThat(response.getDefaultModel()).isNotEmpty();
        assertThat(response.getModels()).hasSize(3);

        ModelItemDto item = response.getModels().stream()
                .filter(m -> "qwen3-8b".equals(m.getId()))
                .findFirst().orElseThrow();
        assertThat(item.getDisplayName()).isEqualTo("Qwen3 8B");
        assertThat(item.getProvider()).isEqualTo("OLLAMA");
        assertThat(item.isEnabled()).isTrue();
        assertThat(item.isAvailable()).isTrue();
        assertThat(item.getCapabilities()).contains("CHAT", "CODING", "INTERVIEW", "MULTILINGUAL");
    }

    @Test
    @DisplayName("getModelsResponse reports available false when provider health check fails")
    void getModelsResponseReportsUnavailableWhenProviderDown() {
        when(ollamaProvider.isAvailable()).thenReturn(false);

        ModelResponse response = modelManager.getModelsResponse();
        assertThat(response.getModels().get(0).isAvailable()).isFalse();
    }
}
