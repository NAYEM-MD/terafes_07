package com.tronai.interview.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message entry within an interview transcript.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TranscriptMessageDto {

    private String id;
    private String sender; // "ai" | "user"
    private String text;
    private String textEn;
    private String timestamp;
    private String audioDuration;

    @JsonProperty("isFollowUp")
    private Boolean isFollowUp;

    public TranscriptMessageDto() {
    }

    public TranscriptMessageDto(String id, String sender, String text, String timestamp, Boolean isFollowUp) {
        this.id = id;
        this.sender = sender;
        this.text = text;
        this.timestamp = timestamp;
        this.isFollowUp = isFollowUp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTextEn() {
        return textEn;
    }

    public void setTextEn(String textEn) {
        this.textEn = textEn;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAudioDuration() {
        return audioDuration;
    }

    public void setAudioDuration(String audioDuration) {
        this.audioDuration = audioDuration;
    }

    @JsonProperty("isFollowUp")
    public Boolean getIsFollowUp() {
        return isFollowUp;
    }

    @JsonProperty("isFollowUp")
    public void setIsFollowUp(Boolean isFollowUp) {
        this.isFollowUp = isFollowUp;
    }
}
