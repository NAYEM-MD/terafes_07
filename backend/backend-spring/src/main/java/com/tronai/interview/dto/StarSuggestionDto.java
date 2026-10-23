package com.tronai.interview.dto;

/**
 * STAR methodology feedback structure.
 */
public class StarSuggestionDto {

    private String situation;
    private String task;
    private String action;
    private String result;

    public StarSuggestionDto() {
        this.situation = "";
        this.task = "";
        this.action = "";
        this.result = "";
    }

    public StarSuggestionDto(String situation, String task, String action, String result) {
        this.situation = situation != null ? situation : "";
        this.task = task != null ? task : "";
        this.action = action != null ? action : "";
        this.result = result != null ? result : "";
    }

    public String getSituation() {
        return situation;
    }

    public void setSituation(String situation) {
        this.situation = situation;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
