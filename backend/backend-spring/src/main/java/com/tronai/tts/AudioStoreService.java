package com.tronai.tts;

import com.tronai.voice.VoiceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory temporary audio clip storage service.
 * Manages short-lived synthesized WAV clips for frontend playback with automatic TTL eviction.
 */
@Service
public class AudioStoreService {

    private final TtsProperties properties;
    private final Map<String, AudioClip> clips = new ConcurrentHashMap<>();

    public AudioStoreService(TtsProperties properties) {
        this.properties = properties;
    }

    public synchronized String storeClip(byte[] data) {
        if (data == null || data.length < 12 || !isRiffWav(data)) {
            throw new VoiceException(HttpStatus.BAD_GATEWAY, "Generated speech was not WAV audio");
        }

        long now = System.currentTimeMillis();
        purge(now);

        String clipId = UUID.randomUUID().toString().replace("-", "");
        clips.put(clipId, new AudioClip(data, now));
        return clipId;
    }

    public synchronized byte[] loadClip(String clipId) {
        long now = System.currentTimeMillis();
        purge(now);

        AudioClip clip = clips.get(clipId);
        if (clip == null) {
            throw new VoiceException(HttpStatus.NOT_FOUND, "Voice audio was not found or has expired");
        }
        return clip.getData();
    }

    public synchronized void clear() {
        clips.clear();
    }

    private void purge(long now) {
        long ttlMillis = (long) properties.getAudioTtlSeconds() * 1000L;

        // Remove expired clips
        clips.entrySet().removeIf(entry -> (now - entry.getValue().getCreatedAt()) > ttlMillis);

        // Enforce maximum stored clips limit
        int overflow = clips.size() - properties.getMaxStoredClips();
        if (overflow > 0) {
            List<Map.Entry<String, AudioClip>> sorted = clips.entrySet().stream()
                    .sorted(Comparator.comparingLong(e -> e.getValue().getCreatedAt()))
                    .limit(overflow)
                    .collect(Collectors.toList());

            for (Map.Entry<String, AudioClip> entry : sorted) {
                clips.remove(entry.getKey());
            }
        }
    }

    private boolean isRiffWav(byte[] data) {
        return data.length >= 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'A' && data[10] == 'V' && data[11] == 'E';
    }

    private static class AudioClip {
        private final byte[] data;
        private final long createdAt;

        public AudioClip(byte[] data, long createdAt) {
            this.data = data;
            this.createdAt = createdAt;
        }

        public byte[] getData() {
            return data;
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }
}
