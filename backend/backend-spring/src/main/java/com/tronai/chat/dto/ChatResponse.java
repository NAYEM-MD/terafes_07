package com.tronai.chat.dto;

import java.util.Objects;

public class ChatResponse {

    private String sessionId;
    private String reply;
    private String content;
    private String language = "en";
    private boolean truncated;
    private String finishReason = "stop";
    private int evalCount;
    private int promptEvalCount;
    private SearchInfo search = SearchInfo.builder().build();
    private String aiProfile = "FAST";
    private String model = "tron-fast";
    /** LOCAL or CLOUD — actual inference path (not the logical profile label in model). */
    private String providerType;
    /** Concrete provider model id used for this turn (e.g. qwen3:8b or groq/…). */
    private String actualModelId;
    private boolean locked = true;

    public ChatResponse() {
    }

    public ChatResponse(String sessionId, String reply, String content, String language, boolean truncated,
                        String finishReason, int evalCount, int promptEvalCount, SearchInfo search) {
        this(sessionId, reply, content, language, truncated, finishReason, evalCount, promptEvalCount, search, "FAST", "tron-fast", true);
    }

    public ChatResponse(String sessionId, String reply, String content, String language, boolean truncated,
                        String finishReason, int evalCount, int promptEvalCount, SearchInfo search,
                        String aiProfile, String model, boolean locked) {
        this.sessionId = sessionId;
        this.reply = reply;
        this.content = content;
        this.language = language != null ? language : "en";
        this.truncated = truncated;
        this.finishReason = finishReason != null ? finishReason : "stop";
        this.evalCount = evalCount;
        this.promptEvalCount = promptEvalCount;
        this.search = search != null ? search : SearchInfo.builder().build();
        this.aiProfile = aiProfile != null ? aiProfile : "FAST";
        this.model = model != null ? model : "tron-fast";
        this.locked = locked;
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

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public int getEvalCount() {
        return evalCount;
    }

    public void setEvalCount(int evalCount) {
        this.evalCount = evalCount;
    }

    public int getPromptEvalCount() {
        return promptEvalCount;
    }

    public void setPromptEvalCount(int promptEvalCount) {
        this.promptEvalCount = promptEvalCount;
    }

    public SearchInfo getSearch() {
        return search;
    }

    public void setSearch(SearchInfo search) {
        this.search = search;
    }

    public String getAiProfile() {
        return aiProfile;
    }

    public void setAiProfile(String aiProfile) {
        this.aiProfile = aiProfile;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getActualModelId() {
        return actualModelId;
    }

    public void setActualModelId(String actualModelId) {
        this.actualModelId = actualModelId;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public static class Builder {
        private String sessionId;
        private String reply;
        private String content;
        private String language = "en";
        private boolean truncated;
        private String finishReason = "stop";
        private int evalCount;
        private int promptEvalCount;
        private SearchInfo search = SearchInfo.builder().build();
        private String aiProfile = "FAST";
        private String model = "tron-fast";
        private String providerType;
        private String actualModelId;
        private boolean locked = true;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder reply(String reply) {
            this.reply = reply;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder truncated(boolean truncated) {
            this.truncated = truncated;
            return this;
        }

        public Builder finishReason(String finishReason) {
            this.finishReason = finishReason;
            return this;
        }

        public Builder evalCount(int evalCount) {
            this.evalCount = evalCount;
            return this;
        }

        public Builder promptEvalCount(int promptEvalCount) {
            this.promptEvalCount = promptEvalCount;
            return this;
        }

        public Builder search(SearchInfo search) {
            this.search = search;
            return this;
        }

        public Builder aiProfile(String aiProfile) {
            this.aiProfile = aiProfile;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder providerType(String providerType) {
            this.providerType = providerType;
            return this;
        }

        public Builder actualModelId(String actualModelId) {
            this.actualModelId = actualModelId;
            return this;
        }

        public Builder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        public ChatResponse build() {
            ChatResponse response = new ChatResponse(sessionId, reply, content, language, truncated, finishReason, evalCount, promptEvalCount, search, aiProfile, model, locked);
            response.setProviderType(providerType);
            response.setActualModelId(actualModelId);
            return response;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatResponse that)) return false;
        return truncated == that.truncated &&
                evalCount == that.evalCount &&
                promptEvalCount == that.promptEvalCount &&
                locked == that.locked &&
                Objects.equals(sessionId, that.sessionId) &&
                Objects.equals(reply, that.reply) &&
                Objects.equals(content, that.content) &&
                Objects.equals(language, that.language) &&
                Objects.equals(finishReason, that.finishReason) &&
                Objects.equals(search, that.search) &&
                Objects.equals(aiProfile, that.aiProfile) &&
                Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, reply, content, language, truncated, finishReason, evalCount, promptEvalCount, search, aiProfile, model, locked);
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
                "sessionId='" + sessionId + '\'' +
                ", reply='" + reply + '\'' +
                ", content='" + content + '\'' +
                ", language='" + language + '\'' +
                ", truncated=" + truncated +
                ", finishReason='" + finishReason + '\'' +
                ", evalCount=" + evalCount +
                ", promptEvalCount=" + promptEvalCount +
                ", search=" + search +
                ", aiProfile='" + aiProfile + '\'' +
                ", model='" + model + '\'' +
                ", locked=" + locked +
                '}';
    }
}
