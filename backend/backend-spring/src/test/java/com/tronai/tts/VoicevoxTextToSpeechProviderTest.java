package com.tronai.tts;

import com.tronai.voice.VoiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class VoicevoxTextToSpeechProviderTest {

    private VoicevoxTextToSpeechProvider provider;
    private TtsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new TtsProperties();
        properties.setBaseUrl("http://127.0.0.1:59998"); // unreachable port
        properties.setTimeoutSeconds(1);
        provider = new VoicevoxTextToSpeechProvider(properties);
    }

    @Test
    @DisplayName("Synthesize rejects null or empty text with 400 Bad Request")
    void synthesizeRejectsEmptyText() {
        VoiceException ex = assertThrows(VoiceException.class, () -> provider.synthesize("   "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Synthesize throws 503 when VOICEVOX is unreachable")
    void synthesizeThrows503WhenUnreachable() {
        VoiceException ex = assertThrows(VoiceException.class, () -> provider.synthesize("こんにちは"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertTrue(ex.getMessage().contains("unavailable"));
    }

    @Test
    @DisplayName("isAvailable returns false when VOICEVOX is unreachable")
    void isAvailableReturnsFalseWhenUnreachable() {
        assertFalse(provider.isAvailable());
    }
}
