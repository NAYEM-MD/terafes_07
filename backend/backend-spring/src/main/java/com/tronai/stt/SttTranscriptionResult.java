package com.tronai.stt;

/**
 * Encapsulates the result of speech transcription.
 */
public class SttTranscriptionResult {

    private final String text;
    private final double duration;
    private final String language;

    public SttTranscriptionResult(String text, double duration, String language) {
        this.text = text;
        this.duration = duration;
        this.language = language;
    }

    public String getText() {
        return text;
    }

    public double getDuration() {
        return duration;
    }

    public String getLanguage() {
        return language;
    }
}
