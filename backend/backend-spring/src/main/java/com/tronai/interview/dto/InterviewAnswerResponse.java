package com.tronai.interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response payload for POST /api/v1/interviews/{interviewId}/answers.
 */
public class InterviewAnswerResponse {

    private TranscriptMessageDto userMessage;
    private TranscriptMessageDto aiFollowUpMessage;
    private int nextQuestionIndex;

    @JsonProperty("isFinished")
    private boolean isFinished;

    public InterviewAnswerResponse() {
    }

    public InterviewAnswerResponse(TranscriptMessageDto userMessage, TranscriptMessageDto aiFollowUpMessage,
                                   int nextQuestionIndex, boolean isFinished) {
        this.userMessage = userMessage;
        this.aiFollowUpMessage = aiFollowUpMessage;
        this.nextQuestionIndex = nextQuestionIndex;
        this.isFinished = isFinished;
    }

    public TranscriptMessageDto getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(TranscriptMessageDto userMessage) {
        this.userMessage = userMessage;
    }

    public TranscriptMessageDto getAiFollowUpMessage() {
        return aiFollowUpMessage;
    }

    public void setAiFollowUpMessage(TranscriptMessageDto aiFollowUpMessage) {
        this.aiFollowUpMessage = aiFollowUpMessage;
    }

    public int getNextQuestionIndex() {
        return nextQuestionIndex;
    }

    public void setNextQuestionIndex(int nextQuestionIndex) {
        this.nextQuestionIndex = nextQuestionIndex;
    }

    @JsonProperty("isFinished")
    public boolean isFinished() {
        return isFinished;
    }

    @JsonProperty("isFinished")
    public void setFinished(boolean finished) {
        isFinished = finished;
    }
}
