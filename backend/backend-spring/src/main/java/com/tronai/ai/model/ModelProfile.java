package com.tronai.ai.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Model profile metadata describing a registered model and its provider configuration.
 */
public class ModelProfile {

    private String id;
    private String displayName;
    private ModelProviderType provider;
    private String providerModelName;
    private Set<ModelCapability> capabilities = new HashSet<>();
    private boolean enabled = true;
    private Integer contextWindow = 4096;
    private Integer maxOutputTokens = 1536;

    public ModelProfile() {
    }

    public ModelProfile(String id, String displayName, ModelProviderType provider, String providerModelName,
                        Set<ModelCapability> capabilities, boolean enabled, Integer contextWindow, Integer maxOutputTokens) {
        this.id = id;
        this.displayName = displayName;
        this.provider = provider;
        this.providerModelName = providerModelName;
        this.capabilities = capabilities != null ? new HashSet<>(capabilities) : new HashSet<>();
        this.enabled = enabled;
        this.contextWindow = contextWindow != null ? contextWindow : 4096;
        this.maxOutputTokens = maxOutputTokens != null ? maxOutputTokens : 1536;
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

    public ModelProviderType getProvider() {
        return provider;
    }

    public void setProvider(ModelProviderType provider) {
        this.provider = provider;
    }

    public String getProviderModelName() {
        return providerModelName;
    }

    public void setProviderModelName(String providerModelName) {
        this.providerModelName = providerModelName;
    }

    public Set<ModelCapability> getCapabilities() {
        return capabilities != null ? Collections.unmodifiableSet(capabilities) : Collections.emptySet();
    }

    public void setCapabilities(Set<ModelCapability> capabilities) {
        this.capabilities = capabilities != null ? new HashSet<>(capabilities) : new HashSet<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getContextWindow() {
        return contextWindow;
    }

    public void setContextWindow(Integer contextWindow) {
        this.contextWindow = contextWindow;
    }

    public Integer getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(Integer maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public static class Builder {
        private String id;
        private String displayName;
        private ModelProviderType provider = ModelProviderType.OLLAMA;
        private String providerModelName;
        private Set<ModelCapability> capabilities = new HashSet<>();
        private boolean enabled = true;
        private Integer contextWindow = 4096;
        private Integer maxOutputTokens = 1536;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder provider(ModelProviderType provider) {
            this.provider = provider;
            return this;
        }

        public Builder providerModelName(String providerModelName) {
            this.providerModelName = providerModelName;
            return this;
        }

        public Builder capabilities(Set<ModelCapability> capabilities) {
            this.capabilities = capabilities != null ? new HashSet<>(capabilities) : new HashSet<>();
            return this;
        }

        public Builder capability(ModelCapability capability) {
            if (this.capabilities == null) {
                this.capabilities = new HashSet<>();
            }
            this.capabilities.add(capability);
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder contextWindow(Integer contextWindow) {
            this.contextWindow = contextWindow;
            return this;
        }

        public Builder maxOutputTokens(Integer maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
            return this;
        }

        public ModelProfile build() {
            return new ModelProfile(id, displayName, provider, providerModelName, capabilities, enabled, contextWindow, maxOutputTokens);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModelProfile that)) return false;
        return enabled == that.enabled &&
                Objects.equals(id, that.id) &&
                Objects.equals(displayName, that.displayName) &&
                provider == that.provider &&
                Objects.equals(providerModelName, that.providerModelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, provider, providerModelName, enabled);
    }

    @Override
    public String toString() {
        return "ModelProfile{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", provider=" + provider +
                ", providerModelName='" + providerModelName + '\'' +
                ", enabled=" + enabled +
                ", contextWindow=" + contextWindow +
                ", maxOutputTokens=" + maxOutputTokens +
                '}';
    }
}
