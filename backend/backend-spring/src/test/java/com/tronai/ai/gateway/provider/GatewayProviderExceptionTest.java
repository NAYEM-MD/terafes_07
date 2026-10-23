package com.tronai.ai.gateway.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.tronai.error.ErrorResponse;
import com.tronai.error.GlobalExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayProviderExceptionTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("GatewayProviderException maps to upstream HTTP status")
    void mapsToHttpStatus() {
        GatewayProviderException ex = new GatewayProviderException(
                GatewayProviderException.RATE_LIMITED,
                429,
                "Rate limited");

        ResponseEntity<ErrorResponse> response = handler.handleGatewayProvider(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Rate limited");
    }
}
