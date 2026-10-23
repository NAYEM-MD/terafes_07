package com.tronai.tts;

/**
 * Text-to-Speech provider abstraction.
 * Decouples TRON business logic from the underlying speech synthesis engine.
 */
public interface TextToSpeechProvider {

    /**
     * Synthesizes given text into WAV audio bytes.
     *
     * @param text Clean, speakable text to synthesize
     * @return Raw WAV binary bytes
     */
    byte[] synthesize(String text);

    /**
     * Checks whether the TTS engine is available and responding.
     *
     * @return true if healthy, false otherwise
     */
    boolean isAvailable();
}
