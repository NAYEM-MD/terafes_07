package com.tronai.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Objects;

public class ChatRequest {

    private String sessionId = "";

    @NotEmpty(message = "messages list must not be empty")
    private List<@Valid ChatMessageIn> messages;

    private String language;

    private String searchMode = "auto";

    private String model;

    private String aiProfile;

    public ChatRequest() {
    }

    public ChatRequest(String sessionId, List<ChatMessageIn> messages, String language, String searchMode) {
        this(sessionId, messages, language, searchMode, null, null);
    }

    public ChatRequest(String sessionId, List<ChatMessageIn> messages, String language, String searchMode, String model) {
        this(sessionId, messages, language, searchMode, model, null);
    }

    public ChatRequest(String sessionId, List<ChatMessageIn> messages, String language, String searchMode, String model, String aiProfile) {
        this.sessionId = sessionId != null ? sessionId : "";
        this.messages = messages;
        this.language = language;
        this.searchMode = searchMode != null ? searchMode : "auto";
        this.model = model;
        this.aiProfile = aiProfile;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<ChatMessageIn> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessageIn> messages) {
        this.messages = messages;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(String searchMode) {
        this.searchMode = searchMode;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAiProfile() {
        return aiProfile;
    }

    public void setAiProfile(String aiProfile) {
        this.aiProfile = aiProfile;
    }

    public static class Builder {
        private String sessionId = "";
        private List<ChatMessageIn> messages;
        private String language;
        private String searchMode = "auto";
        private String model;
        private String aiProfile;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder messages(List<ChatMessageIn> messages) {
            this.messages = messages;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder searchMode(String searchMode) {
            this.searchMode = searchMode;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder aiProfile(String aiProfile) {
            this.aiProfile = aiProfile;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(sessionId, messages, language, searchMode, model, aiProfile);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatRequest that)) return false;
        return Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(messages, that.messages) &&
                Objects.equals(language, that.language) &&
                Objects.equals(searchMode, that.searchMode) &&
                Objects.equals(model, that.model) &&
                Objects.equals(aiProfile, that.aiProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, messages, language, searchMode, model, aiProfile);
    }

    @Override
    public String toString() {
        return "ChatRequest{" +
                "sessionId='" + sessionId + '\'' +
                ", messages=" + messages +
                ", language='" + language + '\'' +
                ", searchMode='" + searchMode + '\'' +
                ", model='" + model + '\'' +
                ", aiProfile='" + aiProfile + '\'' +
                '}';
    }
}
