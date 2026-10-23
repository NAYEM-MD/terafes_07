package com.tronai.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class V1VoiceAnswerResponse {

    @JsonProperty("interviewId")
    private String interviewId;

    @JsonProperty("transcript")
    private String transcript;

    @JsonProperty("nextQuestionText")
    private String nextQuestionText;

    @JsonProperty("nextQuestionIndex")
    private int nextQuestionIndex;

    @JsonProperty("isFinished")
    private boolean isFinished;

    @JsonProperty("audio_url")
    private String audioUrl;

    @JsonProperty("audioContentType")
    private String audioContentType = "audio/wav";

    @JsonProperty("latency_ms")
    private VoiceLatencyDto latencyMs;

    public V1VoiceAnswerResponse() {
    }

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getNextQuestionText() {
        return nextQuestionText;
    }

    public void setNextQuestionText(String nextQuestionText) {
        this.nextQuestionText = nextQuestionText;
    }

    public int getNextQuestionIndex() {
        return nextQuestionIndex;
    }

    public void setNextQuestionIndex(int nextQuestionIndex) {
        this.nextQuestionIndex = nextQuestionIndex;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getAudioContentType() {
        return audioContentType;
    }

    public void setAudioContentType(String audioContentType) {
        this.audioContentType = audioContentType;
    }

    public VoiceLatencyDto getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(VoiceLatencyDto latencyMs) {
        this.latencyMs = latencyMs;
    }
}
