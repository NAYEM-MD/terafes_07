package com.tronai.tts;

import com.tronai.voice.VoiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class AudioStoreServiceTest {

    private AudioStoreService audioStoreService;
    private TtsProperties properties;

    private static final byte[] VALID_WAV = new byte[]{
            'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' '
    };

    @BeforeEach
    void setUp() {
        properties = new TtsProperties();
        properties.setAudioTtlSeconds(600);
        properties.setMaxStoredClips(3);
        audioStoreService = new AudioStoreService(properties);
    }

    @Test
    @DisplayName("Store and load valid WAV clip successfully")
    void storeAndLoadClip() {
        String clipId = audioStoreService.storeClip(VALID_WAV);
        assertNotNull(clipId);
        assertFalse(clipId.isBlank());

        byte[] loaded = audioStoreService.loadClip(clipId);
        assertArrayEquals(VALID_WAV, loaded);
    }

    @Test
    @DisplayName("Reject non-WAV bytes with 502 Bad Gateway")
    void rejectNonWavBytes() {
        byte[] invalidData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        VoiceException ex = assertThrows(VoiceException.class, () -> audioStoreService.storeClip(invalidData));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertTrue(ex.getMessage().contains("WAV"));
    }

    @Test
    @DisplayName("Load non-existent clip throws 404 Not Found")
    void loadNonExistentClipThrows404() {
        VoiceException ex = assertThrows(VoiceException.class, () -> audioStoreService.loadClip("nonexistent"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Evicts oldest clips when exceeding maxStoredClips")
    void evictsOldestOnOverflow() throws InterruptedException {
        String clip1 = audioStoreService.storeClip(VALID_WAV);
        Thread.sleep(5);
        String clip2 = audioStoreService.storeClip(VALID_WAV);
        Thread.sleep(5);
        String clip3 = audioStoreService.storeClip(VALID_WAV);
        Thread.sleep(5);
        String clip4 = audioStoreService.storeClip(VALID_WAV);

        // clip1 should have been evicted because max is 3
        assertThrows(VoiceException.class, () -> audioStoreService.loadClip(clip1));
        assertNotNull(audioStoreService.loadClip(clip4));
    }
}
