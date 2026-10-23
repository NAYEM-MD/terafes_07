package com.tronai.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSummaryDto {
    private String id;
    private String companyName;
    private String jobPosition;
    private String interviewType;
    private String difficulty;
    private int currentQuestionIndex;
    private int questionCount;
    private boolean finished;
    private Integer overallScore;
    private String aiProfile;
    private String modelId;
    private boolean locked;
    private Instant startedAt;
    private Instant completedAt;
}
