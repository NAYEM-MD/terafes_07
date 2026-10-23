package com.tronai.tts;

/**
 * What this deployment can actually hear and speak for one language.
 * A false speech flag means text-only. It is not a promise of audio.
 */
public record SpeechLanguageCapability(
        String language,
        boolean speechToText,
        boolean speechSynthesis,
        String synthesisProvider,
        String detail
) {
}
