package com.tronai.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cloud fallback settings. Disabled unless CLOUD_AI_ENABLED=true.
 * The API key is read from the environment and is never included in logs.
 */
@Component
@ConfigurationProperties(prefix = "cloud.ai")
public class CloudAiProperties {

    private boolean enabled = false;
    private String provider = "";
    private String apiKey = "";
    private String baseUrl = "";
    private String modelFast = "";
    private String modelQuality = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelFast() {
        return modelFast;
    }

    public void setModelFast(String modelFast) {
        this.modelFast = modelFast;
    }

    public String getModelQuality() {
        return modelQuality;
    }

    public void setModelQuality(String modelQuality) {
        this.modelQuality = modelQuality;
    }

    public boolean isConfigured() {
        return enabled
                && notBlank(provider)
                && notBlank(apiKey)
                && notBlank(baseUrl)
                && notBlank(modelFast)
                && notBlank(modelQuality);
    }

    public String modelForProfile(String logicalProfile) {
        if ("QUALITY".equalsIgnoreCase(logicalProfile)) {
            return modelQuality == null ? "" : modelQuality.trim();
        }
        return modelFast == null ? "" : modelFast.trim();
    }

    public String healthStatus() {
        if (!enabled) {
            return "disabled";
        }
        return isConfigured() ? "configured" : "unconfigured";
    }

    @Override
    public String toString() {
        return "CloudAiProperties{enabled=" + enabled
                + ", provider=" + provider
                + ", baseUrl=" + baseUrl
                + ", modelFast=" + modelFast
                + ", modelQuality=" + modelQuality
                + "}";
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
