package com.tronai.tts;

import com.tronai.language.ExperienceLanguage;
import com.tronai.voice.VoiceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Japanese speech stays on local VOICEVOX. Other languages use the multilingual
 * provider only when that provider is configured. Otherwise the reply stays text.
 */
@Component
public class SpeechSynthesisRouter {

    private final TextToSpeechProvider japaneseProvider;
    private final MultilingualTtsProvider multilingualProvider;

    public SpeechSynthesisRouter(TextToSpeechProvider japaneseProvider, MultilingualTtsProperties multilingualProperties) {
        this.japaneseProvider = japaneseProvider;
        this.multilingualProvider = new MultilingualTtsProvider(multilingualProperties);
    }

    public SpeechLanguageCapability capability(String requestedLanguage) {
        ExperienceLanguage language = ExperienceLanguage.parseOrDefault(requestedLanguage, ExperienceLanguage.JA);
        if (language == ExperienceLanguage.JA && japaneseProvider.isAvailable()) {
            return new SpeechLanguageCapability(language.code(), true, true, "voicevox",
                    "Japanese speech from local VOICEVOX");
        }
        if (language == ExperienceLanguage.JA) {
            return new SpeechLanguageCapability(language.code(), true, false, null,
                    "VOICEVOX is not available. The reply stays text.");
        }
        if (multilingualProvider.supports(language)) {
            return new SpeechLanguageCapability(language.code(), true, true, "multilingual",
                    "Speech from the configured multilingual TTS provider");
        }
        return new SpeechLanguageCapability(language.code(), true, false, null,
                "No multilingual TTS provider is configured. VOICEVOX is Japanese-only, so this reply stays text.");
    }

    public Optional<byte[]> synthesizeIfSupported(String text, String requestedLanguage) {
        ExperienceLanguage language = ExperienceLanguage.parseOrDefault(requestedLanguage, ExperienceLanguage.JA);
        SpeechLanguageCapability capability = capability(language.code());
        if (!capability.speechSynthesis()) {
            return Optional.empty();
        }
        if ("voicevox".equals(capability.synthesisProvider())) {
            return Optional.of(japaneseProvider.synthesize(text));
        }
        return Optional.of(multilingualProvider.synthesize(text, language));
    }

    public byte[] synthesizeRequired(String text, String requestedLanguage) {
        return synthesizeIfSupported(text, requestedLanguage).orElseThrow(() ->
                new VoiceException(HttpStatus.UNPROCESSABLE_ENTITY, capability(requestedLanguage).detail()));
    }
}
