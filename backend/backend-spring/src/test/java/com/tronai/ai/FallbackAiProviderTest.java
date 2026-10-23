package com.tronai.ai;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tronai.ai.model.ModelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FallbackAiProviderTest {

    private static final String SECRET = "sk-test-key-must-not-leak";

    private AiProvider local;
    private AiProvider cloud;
    private ModelProperties modelProperties;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void setUp() {
        local = mock(AiProvider.class);
        cloud = mock(AiProvider.class);
        when(local.getProviderName()).thenReturn("ollama");
        modelProperties = new ModelProperties();
        modelProperties.setFastModel("qwen3:8b");
        modelProperties.setQualityModel("gemma3:12b");
        logger = (Logger) LoggerFactory.getLogger(FallbackAiProvider.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        GenerationTraceHolder.clear();
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        GenerationTraceHolder.clear();
    }

    @Test
    @DisplayName("Local success does not call cloud and records LOCAL")
    void localSuccessDoesNotCallCloud() {
        when(local.generate(any())).thenReturn(ChatGenerationResult.builder().text("local-answer").build());

        ChatGenerationResult result = fallback(disabledCloud()).generate(qualityRequest());

        verify(cloud, never()).generate(any());
        assertThat(result.getText()).isEqualTo("local-answer");
        assertThat(result.getLogicalProfile()).isEqualTo("QUALITY");
        assertThat(result.getProviderType()).isEqualTo(ProviderType.LOCAL);
        assertThat(result.getActualModelId()).isEqualTo("gemma3:12b");
        assertThat(GenerationTraceHolder.current().providerType()).isEqualTo(ProviderType.LOCAL);
        assertLogsDoNotContainSecret();
    }

    @Test
    @DisplayName("Local failure with cloud disabled returns the privacy error and does not call cloud")
    void localFailureCloudDisabled() {
        when(local.generate(any())).thenThrow(new AiProviderUnavailableException("connection refused"));

        assertThatThrownBy(() -> fallback(disabledCloud()).generate(qualityRequest()))
                .isInstanceOf(AiProviderUnavailableException.class)
                .hasMessage(FallbackAiProvider.LOCAL_UNAVAILABLE_DETAIL);

        verify(cloud, never()).generate(any());
        assertThat(GenerationTraceHolder.current()).isNull();
        assertLogsDoNotContainSecret();
    }

    @Test
    @DisplayName("Local failure with cloud enabled calls cloud and keeps QUALITY")
    void localFailureCloudEnabled() {
        when(local.generate(any())).thenThrow(new AiProviderUnavailableException("connection refused"));
        when(cloud.generate(any())).thenReturn(ChatGenerationResult.builder().text("cloud-answer").build());

        ChatGenerationResult result = fallback(enabledCloud()).generate(qualityRequest());

        verify(cloud).generate(any());
        assertThat(result.getText()).isEqualTo("cloud-answer");
        assertThat(result.getLogicalProfile()).isEqualTo("QUALITY");
        assertThat(result.getProviderType()).isEqualTo(ProviderType.CLOUD);
        assertThat(result.getActualModelId()).isEqualTo("cloud-quality-model");
        assertThat(result.getText()).doesNotContain(SECRET);
        assertThat(GenerationTraceHolder.current().logicalProfile()).isEqualTo("QUALITY");
        assertLogsDoNotContainSecret();
    }

    @Test
    @DisplayName("Cloud failure becomes a clean error without the API key")
    void cloudFailureIsClean() {
        when(local.generate(any())).thenThrow(new AiProviderUnavailableException("down"));
        when(cloud.generate(any())).thenThrow(new AiProviderException("provider exploded " + SECRET));

        assertThatThrownBy(() -> fallback(enabledCloud()).generate(fastRequest()))
                .isInstanceOf(CloudAiException.class)
                .hasMessage("Cloud AI provider failed.")
                .hasMessageNotContaining(SECRET);

        assertLogsDoNotContainSecret();
    }

    @Test
    @DisplayName("FAST profile stays FAST when cloud serves the request")
    void fastProfileStaysFast() {
        when(local.generate(any())).thenThrow(new AiProviderUnavailableException("down"));
        when(cloud.generate(any())).thenReturn(ChatGenerationResult.builder().text("fast-cloud").build());

        ChatGenerationResult result = fallback(enabledCloud()).generate(fastRequest());

        assertThat(result.getLogicalProfile()).isEqualTo("FAST");
        assertThat(result.getProviderType()).isEqualTo(ProviderType.CLOUD);
        assertThat(result.getActualModelId()).isEqualTo("cloud-fast-model");
    }

    @Test
    @DisplayName("Real cloud stub does not connect and does not expose the API key")
    void realCloudStubHidesKey() {
        CloudAiProperties properties = enabledCloud();
        CloudAiProvider provider = new CloudAiProvider(properties);

        assertThat(properties.toString()).doesNotContain(SECRET);
        assertThat(provider.isAvailable()).isTrue();
        assertThatThrownBy(() -> provider.generate(fastRequest()))
                .isInstanceOf(CloudAiException.class)
                .hasMessage("Cloud AI provider is not connected.")
                .hasMessageNotContaining(SECRET);
        assertThat(logger.getLevel()).isNotEqualTo(Level.OFF);
    }

    private FallbackAiProvider fallback(CloudAiProperties properties) {
        return new FallbackAiProvider(local, cloud, properties, modelProperties);
    }

    private static CloudAiProperties disabledCloud() {
        CloudAiProperties properties = new CloudAiProperties();
        properties.setEnabled(false);
        properties.setApiKey(SECRET);
        return properties;
    }

    private static CloudAiProperties enabledCloud() {
        CloudAiProperties properties = new CloudAiProperties();
        properties.setEnabled(true);
        properties.setProvider("example-provider");
        properties.setApiKey(SECRET);
        properties.setBaseUrl("https://cloud.example.internal");
        properties.setModelFast("cloud-fast-model");
        properties.setModelQuality("cloud-quality-model");
        return properties;
    }

    private static ChatGenerationRequest qualityRequest() {
        return ChatGenerationRequest.builder()
                .model("gemma3:12b")
                .logicalProfile("QUALITY")
                .build();
    }

    private static ChatGenerationRequest fastRequest() {
        return ChatGenerationRequest.builder()
                .model("qwen3:8b")
                .logicalProfile("FAST")
                .build();
    }

    private void assertLogsDoNotContainSecret() {
        assertThat(logs.list).allSatisfy(event -> assertThat(event.getFormattedMessage()).doesNotContain(SECRET));
    }
}
