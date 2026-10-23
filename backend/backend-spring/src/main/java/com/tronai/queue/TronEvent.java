package com.tronai.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Standard event message published to RabbitMQ.
 * Strictly avoids private data (passwords, JWTs, CV binaries, audio).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TronEvent implements Serializable {
    private String eventId;
    private String eventType;
    private Long userId;
    private String resourceId;
    private Instant createdAt;
    private Map<String, Object> metadata;
}
