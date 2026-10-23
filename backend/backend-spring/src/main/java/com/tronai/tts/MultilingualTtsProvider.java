package com.tronai.tts;

import com.tronai.language.ExperienceLanguage;
import com.tronai.voice.VoiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Environment-driven multilingual speech client. It stays inactive until
 * {@code MULTILINGUAL_TTS_BASE_URL} is set. The request is {@code POST {base}/synthesize}
 * with JSON {@code text} and {@code language}, and the response is WAV bytes.
 */
public class MultilingualTtsProvider {

    private final MultilingualTtsProperties properties;
    private final RestClient restClient;

    public MultilingualTtsProvider(MultilingualTtsProperties properties) {
        this.properties = properties;
        if (!properties.isConfigured()) {
            this.restClient = null;
            return;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(Math.max(properties.getTimeoutSeconds(), 5)));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public boolean supports(ExperienceLanguage language) {
        return properties.isConfigured() && language != null;
    }

    public byte[] synthesize(String text, ExperienceLanguage language) {
        if (!supports(language)) {
            throw new VoiceException(HttpStatus.UNPROCESSABLE_ENTITY, "Multilingual speech is not configured");
        }
        byte[] audio = restClient.post()
                .uri("/synthesize")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", text, "language", language.code()))
                .retrieve()
                .body(byte[].class);
        if (audio == null || audio.length == 0) {
            throw new VoiceException(HttpStatus.SERVICE_UNAVAILABLE, "Multilingual speech provider returned no audio");
        }
        return audio;
    }
}
