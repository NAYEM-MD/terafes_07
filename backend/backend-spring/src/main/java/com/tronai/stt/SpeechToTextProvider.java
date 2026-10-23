package com.tronai.stt;

import com.tronai.ai.profile.AiProfile;

/**
 * Speech-to-Text provider abstraction.
 * Decouples TRON business logic from the underlying speech engine (faster-whisper, cloud STT, etc.).
 */
public interface SpeechToTextProvider {

    /**
     * Transcribes input audio data with profile specification (FAST or QUALITY).
     *
     * @param filename    Original filename or synthetic name (e.g., "mic.wav")
     * @param contentType MIME type of audio (e.g., "audio/wav", "audio/webm")
     * @param data        Binary audio payload
     * @param language    Target language code (e.g., "ja", "en")
     * @param profile     Logical AI profile (FAST or QUALITY)
     * @return SttTranscriptionResult containing transcript, duration, and detected language
     */
    SttTranscriptionResult transcribe(String filename, String contentType, byte[] data, String language, AiProfile profile);

    /**
     * Default overload for backward compatibility defaulting to FAST profile.
     */
    default SttTranscriptionResult transcribe(String filename, String contentType, byte[] data, String language) {
        return transcribe(filename, contentType, data, language, AiProfile.FAST);
    }

    /**
     * Checks whether the STT provider is available and responding.
     *
     * @return true if healthy, false otherwise
     */
    boolean isAvailable();
}
