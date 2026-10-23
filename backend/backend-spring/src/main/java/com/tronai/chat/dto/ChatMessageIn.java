package com.tronai.chat.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

public class ChatMessageIn {

    @NotBlank(message = "role is required")
    private String role;

    @NotBlank(message = "content must not be blank")
    private String content;

    private String id;
    private String createdAt;

    public ChatMessageIn() {
    }

    public ChatMessageIn(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public ChatMessageIn(String role, String content, String id, String createdAt) {
        this.role = role;
        this.content = content;
        this.id = id;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public static class Builder {
        private String role;
        private String content;
        private String id;
        private String createdAt;

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ChatMessageIn build() {
            return new ChatMessageIn(role, content, id, createdAt);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatMessageIn that)) return false;
        return Objects.equals(role, that.role) &&
                Objects.equals(content, that.content) &&
                Objects.equals(id, that.id) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content, id, createdAt);
    }

    @Override
    public String toString() {
        return "ChatMessageIn{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", id='" + id + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
