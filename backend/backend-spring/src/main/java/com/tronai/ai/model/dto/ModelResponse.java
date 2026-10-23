package com.tronai.ai.model.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Top-level response for GET /api/v1/models.
 */
public class ModelResponse {

    private String defaultModel = "qwen3-8b";
    private List<ModelItemDto> models = new ArrayList<>();

    public ModelResponse() {
    }

    public ModelResponse(String defaultModel, List<ModelItemDto> models) {
        this.defaultModel = defaultModel;
        this.models = models != null ? models : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public List<ModelItemDto> getModels() {
        return models;
    }

    public void setModels(List<ModelItemDto> models) {
        this.models = models != null ? models : new ArrayList<>();
    }

    public static class Builder {
        private String defaultModel = "qwen3-8b";
        private List<ModelItemDto> models = new ArrayList<>();

        public Builder defaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
            return this;
        }

        public Builder models(List<ModelItemDto> models) {
            this.models = models != null ? models : new ArrayList<>();
            return this;
        }

        public ModelResponse build() {
            return new ModelResponse(defaultModel, models);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModelResponse that)) return false;
        return Objects.equals(defaultModel, that.defaultModel) &&
                Objects.equals(models, that.models);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultModel, models);
    }

    @Override
    public String toString() {
        return "ModelResponse{" +
                "defaultModel='" + defaultModel + '\'' +
                ", models=" + models +
                '}';
    }
}
