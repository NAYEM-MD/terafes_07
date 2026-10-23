package com.tronai.tts;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "voicevox")
public class TtsProperties {

    private String baseUrl = "http://127.0.0.1:50021";
    private int speakerId = 13;
    private int timeoutSeconds = 30;
    private int audioTtlSeconds = 600;
    private int maxStoredClips = 20;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getSpeakerId() {
        return speakerId;
    }

    public void setSpeakerId(int speakerId) {
        this.speakerId = speakerId;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getAudioTtlSeconds() {
        return audioTtlSeconds;
    }

    public void setAudioTtlSeconds(int audioTtlSeconds) {
        this.audioTtlSeconds = audioTtlSeconds;
    }

    public int getMaxStoredClips() {
        return maxStoredClips;
    }

    public void setMaxStoredClips(int maxStoredClips) {
        this.maxStoredClips = maxStoredClips;
    }
}
