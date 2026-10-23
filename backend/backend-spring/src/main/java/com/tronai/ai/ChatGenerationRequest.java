package com.tronai.ai;

import java.util.List;
import java.util.Objects;

public class ChatGenerationRequest {
    private String model;
    private List<ChatMessage> messages;
    private Double temperature = 0.4;
    private Integer numPredict = 1024;
    private boolean jsonMode;
    private String logicalProfile;

    public ChatGenerationRequest() {
    }

    public ChatGenerationRequest(String model, List<ChatMessage> messages, Double temperature, Integer numPredict, boolean jsonMode) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature != null ? temperature : 0.4;
        this.numPredict = numPredict != null ? numPredict : 1024;
        this.jsonMode = jsonMode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getNumPredict() {
        return numPredict;
    }

    public void setNumPredict(Integer numPredict) {
        this.numPredict = numPredict;
    }

    public boolean isJsonMode() {
        return jsonMode;
    }

    public void setJsonMode(boolean jsonMode) {
        this.jsonMode = jsonMode;
    }

    public String getLogicalProfile() {
        return logicalProfile;
    }

    public void setLogicalProfile(String logicalProfile) {
        this.logicalProfile = logicalProfile;
    }

    public static class Builder {
        private String model;
        private List<ChatMessage> messages;
        private Double temperature = 0.4;
        private Integer numPredict = 1024;
        private boolean jsonMode;
        private String logicalProfile;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder numPredict(Integer numPredict) {
            this.numPredict = numPredict;
            return this;
        }

        public Builder jsonMode(boolean jsonMode) {
            this.jsonMode = jsonMode;
            return this;
        }

        public Builder logicalProfile(String logicalProfile) {
            this.logicalProfile = logicalProfile;
            return this;
        }

        public ChatGenerationRequest build() {
            ChatGenerationRequest request = new ChatGenerationRequest(model, messages, temperature, numPredict, jsonMode);
            request.setLogicalProfile(logicalProfile);
            return request;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatGenerationRequest that)) return false;
        return jsonMode == that.jsonMode &&
                Objects.equals(model, that.model) &&
                Objects.equals(messages, that.messages) &&
                Objects.equals(temperature, that.temperature) &&
                Objects.equals(numPredict, that.numPredict);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, messages, temperature, numPredict, jsonMode);
    }

    @Override
    public String toString() {
        return "ChatGenerationRequest{" +
                "model='" + model + '\'' +
                ", messages=" + messages +
                ", temperature=" + temperature +
                ", numPredict=" + numPredict +
                ", jsonMode=" + jsonMode +
                '}';
    }
}
