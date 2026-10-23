package com.tronai.voice.dto;

public class VoiceLatencyDto {

    private int stt;
    private int llm;
    private int tts;
    private int total;

    public VoiceLatencyDto() {
    }

    public VoiceLatencyDto(int stt, int llm, int tts, int total) {
        this.stt = stt;
        this.llm = llm;
        this.tts = tts;
        this.total = total;
    }

    public int getStt() {
        return stt;
    }

    public void setStt(int stt) {
        this.stt = stt;
    }

    public int getLlm() {
        return llm;
    }

    public void setLlm(int llm) {
        this.llm = llm;
    }

    public int getTts() {
        return tts;
    }

    public void setTts(int tts) {
        this.tts = tts;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
