package com.tronai.stt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tronai.voice.VoiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class RemoteWhisperSpeechToTextProviderTest {

    private RemoteWhisperSpeechToTextProvider provider;
    private SttProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SttProperties();
        properties.setBaseUrl("http://127.0.0.1:59999"); // unreachable port
        properties.setTimeoutSeconds(1);
        provider = new RemoteWhisperSpeechToTextProvider(properties);
    }

    @Test
    @DisplayName("Transcribe rejects null or empty audio bytes with 400 Bad Request")
    void transcribeRejectsEmptyAudio() {
        VoiceException ex = assertThrows(VoiceException.class, () ->
                provider.transcribe("test.wav", "audio/wav", new byte[0], "ja")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Transcribe rejects oversized audio with 413 Payload Too Large")
    void transcribeRejectsOversizedAudio() {
        byte[] oversized = new byte[5 * 1024 * 1024]; // 5MB
        VoiceException ex = assertThrows(VoiceException.class, () ->
                provider.transcribe("test.wav", "audio/wav", oversized, "ja")
        );
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, ex.getStatus());
        assertTrue(ex.getMessage().contains("4MB"));
    }

    @Test
    @DisplayName("Transcribe throws 503 when STT worker is unreachable")
    void transcribeThrows503WhenUnreachable() {
        byte[] validWav = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'};
        VoiceException ex = assertThrows(VoiceException.class, () ->
                provider.transcribe("test.wav", "audio/wav", validWav, "ja")
        );
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertTrue(ex.getMessage().contains("unavailable"));
    }

    @Test
    @DisplayName("isAvailable returns false when STT worker is unreachable")
    void isAvailableReturnsFalseWhenUnreachable() {
        assertFalse(provider.isAvailable());
    }
}
