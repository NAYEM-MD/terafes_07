package com.tronai.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDetailDto {
    private String id;
    private InterviewConfigDto config;
    private int currentQuestionIndex;
    private int questionCount;
    private boolean finished;
    private String aiProfile;
    private String modelId;
    private boolean locked;
    private Instant startedAt;
    private Instant completedAt;
    private List<TranscriptMessageDto> transcript;
}
