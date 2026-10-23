package com.tronai.ai.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for TRON model management and AI performance profiles.
 */
@Component
@ConfigurationProperties(prefix = "tron")
public class ModelProperties {

    /**
     * Default logical model ID to use when none is specified.
     */
    private String defaultModel = "tron-fast";

    /**
     * Provider model name for FAST AI profile (e.g. qwen3:8b).
     */
    private String fastModel = "qwen3:8b";

    /**
     * Provider model name for QUALITY AI profile (e.g. gemma3:12b).
     */
    private String qualityModel = "gemma3:12b";

    /**
     * Default AI profile (FAST).
     */
    private String defaultProfile = "FAST";

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getFastModel() {
        return fastModel != null && !fastModel.isBlank() ? fastModel.trim() : "qwen3:8b";
    }

    public void setFastModel(String fastModel) {
        this.fastModel = fastModel;
    }

    public String getQualityModel() {
        return qualityModel != null && !qualityModel.isBlank() ? qualityModel.trim() : "gemma3:12b";
    }

    public void setQualityModel(String qualityModel) {
        this.qualityModel = qualityModel;
    }

    public String getDefaultProfile() {
        return defaultProfile != null && !defaultProfile.isBlank() ? defaultProfile.trim() : "FAST";
    }

    public void setDefaultProfile(String defaultProfile) {
        this.defaultProfile = defaultProfile;
    }
}
