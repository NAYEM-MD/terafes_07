package com.tronai.tts;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Optional multilingual speech endpoint. Empty base URL means it is not configured.
 * No vendor client is hardcoded.
 */
@Component
@ConfigurationProperties(prefix = "tts.multilingual")
public class MultilingualTtsProperties {

    private String baseUrl = "";
    private int timeoutSeconds = 30;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
