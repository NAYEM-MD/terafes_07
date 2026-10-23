package com.tronai.voice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpeakRequest {

    @JsonProperty("text")
    private String text;

    public SpeakRequest() {
    }

    public SpeakRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
