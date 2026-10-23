package com.tronai.interview.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Question card structure matching frontend contract.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterviewQuestionDto {

    private String id;
    private int questionNumber;
    private Integer totalQuestions;
    private String category;
    private String categoryJa;
    private String textJa;
    private String textEn;
    private List<String> expectedKeypoints;
    private String suggestedFollowUp;
    private String difficulty;

    public InterviewQuestionDto() {
    }

    public InterviewQuestionDto(String id, int questionNumber, Integer totalQuestions, String category,
                                String categoryJa, String textJa, String textEn, String difficulty) {
        this.id = id;
        this.questionNumber = questionNumber;
        this.totalQuestions = totalQuestions;
        this.category = category;
        this.categoryJa = categoryJa;
        this.textJa = textJa;
        this.textEn = textEn;
        this.difficulty = difficulty;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryJa() {
        return categoryJa;
    }

    public void setCategoryJa(String categoryJa) {
        this.categoryJa = categoryJa;
    }

    public String getTextJa() {
        return textJa;
    }

    public void setTextJa(String textJa) {
        this.textJa = textJa;
    }

    public String getTextEn() {
        return textEn;
    }

    public void setTextEn(String textEn) {
        this.textEn = textEn;
    }

    public List<String> getExpectedKeypoints() {
        return expectedKeypoints;
    }

    public void setExpectedKeypoints(List<String> expectedKeypoints) {
        this.expectedKeypoints = expectedKeypoints;
    }

    public String getSuggestedFollowUp() {
        return suggestedFollowUp;
    }

    public void setSuggestedFollowUp(String suggestedFollowUp) {
        this.suggestedFollowUp = suggestedFollowUp;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
