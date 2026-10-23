package com.tronai.tts;

import com.fasterxml.jackson.databind.JsonNode;
import com.tronai.voice.VoiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

/**
 * Text-to-Speech provider communicating with local VOICEVOX engine (:50021).
 */
@Component
public class VoicevoxTextToSpeechProvider implements TextToSpeechProvider {

    private static final Logger log = LoggerFactory.getLogger(VoicevoxTextToSpeechProvider.class);

    private final TtsProperties properties;
    private final RestClient restClient;

    public VoicevoxTextToSpeechProvider(TtsProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public byte[] synthesize(String text) {
        String cleanText = (text != null) ? text.trim() : "";
        if (cleanText.isEmpty()) {
            throw new VoiceException(HttpStatus.BAD_REQUEST, "Text to synthesize cannot be empty");
        }

        try {
            // 1. Create Audio Query
            URI queryUri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                    .path("/audio_query")
                    .queryParam("text", cleanText)
                    .queryParam("speaker", properties.getSpeakerId())
                    .build()
                    .encode()
                    .toUri();

            String queryJson = restClient.post()
                    .uri(queryUri)
                    .retrieve()
                    .body(String.class);

            if (queryJson == null || queryJson.isBlank()) {
                log.warn("VOICEVOX audio_query returned empty body");
                throw new VoiceException(HttpStatus.SERVICE_UNAVAILABLE, "VOICEVOX is unavailable");
            }

            // 2. Synthesize Audio
            URI synthUri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                    .path("/synthesis")
                    .queryParam("speaker", properties.getSpeakerId())
                    .build()
                    .encode()
                    .toUri();

            byte[] audioBytes = restClient.post()
                    .uri(synthUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(queryJson)
                    .retrieve()
                    .body(byte[].class);

            if (audioBytes == null || audioBytes.length < 12 || !isRiffWav(audioBytes)) {
                log.warn("VOICEVOX synthesis did not return valid WAV audio");
                throw new VoiceException(HttpStatus.SERVICE_UNAVAILABLE, "VOICEVOX is unavailable");
            }

            return audioBytes;

        } catch (RestClientResponseException ex) {
            log.warn("VOICEVOX HTTP {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new VoiceException(HttpStatus.SERVICE_UNAVAILABLE, "VOICEVOX is unavailable", ex);

        } catch (ResourceAccessException ex) {
            log.error("VOICEVOX connection or timeout error: {}", ex.getMessage());
            if (ex.getCause() instanceof java.net.SocketTimeoutException) {
                throw new VoiceException(HttpStatus.GATEWAY_TIMEOUT, "VOICEVOX timed out", ex);
            }
            throw new VoiceException(HttpStatus.SERVICE_UNAVAILABLE, "VOICEVOX is unavailable", ex);

        } catch (VoiceException ex) {
            throw ex;

        } catch (Exception ex) {
            log.error("VOICEVOX unexpected synthesis error: {}", ex.getMessage(), ex);
            throw new VoiceException(HttpStatus.SERVICE_UNAVAILABLE, "VOICEVOX is unavailable", ex);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            SimpleClientHttpRequestFactory quickFactory = new SimpleClientHttpRequestFactory();
            quickFactory.setConnectTimeout(Duration.ofSeconds(2));
            quickFactory.setReadTimeout(Duration.ofSeconds(2));

            RestClient quickClient = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .requestFactory(quickFactory)
                    .build();

            String version = quickClient.get()
                    .uri("/version")
                    .retrieve()
                    .body(String.class);

            return version != null && !version.isBlank();
        } catch (Exception ex) {
            log.debug("VOICEVOX health check failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isRiffWav(byte[] data) {
        return data.length >= 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'A' && data[10] == 'V' && data[11] == 'E';
    }
}
