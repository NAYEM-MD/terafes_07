package com.tronai.auth.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleOAuthCredentialsTest {

    @Test
    @DisplayName("Google is enabled only when both values exist and neither is the removed fallback")
    void configuredRequiresBothRealValues() {
        assertThat(GoogleOAuthCredentials.isConfigured(null, null)).isFalse();
        assertThat(GoogleOAuthCredentials.isConfigured("real.apps.googleusercontent.com", " ")).isFalse();
        assertThat(GoogleOAuthCredentials.isConfigured("", "secret")).isFalse();
        assertThat(GoogleOAuthCredentials.isConfigured(
                GoogleOAuthCredentials.REMOVED_FAKE_CLIENT_ID, "secret")).isFalse();
        assertThat(GoogleOAuthCredentials.isConfigured(
                "real.apps.googleusercontent.com", GoogleOAuthCredentials.REMOVED_FAKE_CLIENT_SECRET)).isFalse();
        assertThat(GoogleOAuthCredentials.isConfigured(
                "real.apps.googleusercontent.com", "real-secret")).isTrue();
    }
}
