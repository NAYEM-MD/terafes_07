package com.tronai.ai.gateway.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.ai.gateway.AiGatewayProperties;
import com.tronai.ai.gateway.ProviderHealthStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudflareWorkersAiProviderTest {

    @Test
    @DisplayName("Token without account id is UNAVAILABLE with clear detail")
    void missingAccountIdIsUnavailable() {
        AiGatewayProperties properties = new AiGatewayProperties();
        properties.setCloudflareApiToken("cf-token-present");
        properties.setCloudflareAccountId("");

        CloudflareWorkersAiProvider provider = new CloudflareWorkersAiProvider(properties, new ObjectMapper());

        assertThat(provider.isConfigured()).isFalse();
        assertThat(provider.health()).isEqualTo(ProviderHealthStatus.UNAVAILABLE);
        assertThat(provider.healthDetail()).containsIgnoringCase("account id");
    }

    @Test
    @DisplayName("No Cloudflare credentials yields DISABLED")
    void missingCredentialsDisabled() {
        AiGatewayProperties properties = new AiGatewayProperties();
        CloudflareWorkersAiProvider provider = new CloudflareWorkersAiProvider(properties, new ObjectMapper());
        assertThat(provider.health()).isEqualTo(ProviderHealthStatus.DISABLED);
    }
}
