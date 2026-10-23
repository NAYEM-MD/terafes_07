package com.tronai.interview.dto;

import java.util.List;

/**
 * Response returned from POST /api/v1/interviews/start.
 */
public class InterviewStartResponse {

    private String interviewId;
    private InterviewConfigDto config;
    private List<InterviewQuestionDto> questions;
    private int currentQuestionIndex;
    private List<TranscriptMessageDto> transcript;
    private String aiProfile;
    private boolean locked = true;

    public InterviewStartResponse() {
    }

    public InterviewStartResponse(String interviewId, InterviewConfigDto config, List<InterviewQuestionDto> questions,
                                  int currentQuestionIndex, List<TranscriptMessageDto> transcript) {
        this(interviewId, config, questions, currentQuestionIndex, transcript,
                (config != null && config.getAiProfile() != null) ? config.getAiProfile() : "FAST", true);
    }

    public InterviewStartResponse(String interviewId, InterviewConfigDto config, List<InterviewQuestionDto> questions,
                                  int currentQuestionIndex, List<TranscriptMessageDto> transcript, String aiProfile, boolean locked) {
        this.interviewId = interviewId;
        this.config = config;
        this.questions = questions;
        this.currentQuestionIndex = currentQuestionIndex;
        this.transcript = transcript;
        this.aiProfile = aiProfile;
        this.locked = locked;
    }

    public String getAiProfile() {
        return aiProfile;
    }

    public void setAiProfile(String aiProfile) {
        this.aiProfile = aiProfile;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public InterviewConfigDto getConfig() {
        return config;
    }

    public void setConfig(InterviewConfigDto config) {
        this.config = config;
    }

    public List<InterviewQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<InterviewQuestionDto> questions) {
        this.questions = questions;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public List<TranscriptMessageDto> getTranscript() {
        return transcript;
    }

    public void setTranscript(List<TranscriptMessageDto> transcript) {
        this.transcript = transcript;
    }
}
