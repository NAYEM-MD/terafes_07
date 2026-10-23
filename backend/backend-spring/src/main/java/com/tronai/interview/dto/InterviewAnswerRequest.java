package com.tronai.interview.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Request payload for POST /api/v1/interviews/{interviewId}/answers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewAnswerRequest {

    private Integer questionIndex;
    private String answerText;

    public InterviewAnswerRequest() {
    }

    public InterviewAnswerRequest(Integer questionIndex, String answerText) {
        this.questionIndex = questionIndex;
        this.answerText = answerText;
    }

    public Integer getQuestionIndex() {
        return questionIndex;
    }

    public void setQuestionIndex(Integer questionIndex) {
        this.questionIndex = questionIndex;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
}
