package com.tronai.ai.model.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Metadata representation of a model returned in GET /api/v1/models.
 */
public class ModelItemDto {

    private String id;
    private String displayName;
    private String provider;
    private List<String> capabilities = new ArrayList<>();
    private boolean enabled;
    private boolean available;

    public ModelItemDto() {
    }

    public ModelItemDto(String id, String displayName, String provider, List<String> capabilities, boolean enabled, boolean available) {
        this.id = id;
        this.displayName = displayName;
        this.provider = provider;
        this.capabilities = capabilities != null ? capabilities : new ArrayList<>();
        this.enabled = enabled;
        this.available = available;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities != null ? capabilities : new ArrayList<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public static class Builder {
        private String id;
        private String displayName;
        private String provider;
        private List<String> capabilities = new ArrayList<>();
        private boolean enabled;
        private boolean available;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder capabilities(List<String> capabilities) {
            this.capabilities = capabilities != null ? capabilities : new ArrayList<>();
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder available(boolean available) {
            this.available = available;
            return this;
        }

        public ModelItemDto build() {
            return new ModelItemDto(id, displayName, provider, capabilities, enabled, available);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModelItemDto that)) return false;
        return enabled == that.enabled &&
                available == that.available &&
                Objects.equals(id, that.id) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(provider, that.provider) &&
                Objects.equals(capabilities, that.capabilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, provider, capabilities, enabled, available);
    }

    @Override
    public String toString() {
        return "ModelItemDto{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", provider='" + provider + '\'' +
                ", capabilities=" + capabilities +
                ", enabled=" + enabled +
                ", available=" + available +
                '}';
    }
}
