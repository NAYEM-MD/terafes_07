package com.tronai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionSummaryDto {
    private Long id;
    private String clientSessionId;
    private String title;
    private String modelId;
    private String aiProfile;
    private String modelDisplayName;
    private boolean locked;
    private Instant createdAt;
    private Instant updatedAt;
    private int messageCount;
}
