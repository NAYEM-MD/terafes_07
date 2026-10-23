package com.tronai.interview;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "interview_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private InterviewSessionEntity session;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "communication_score", nullable = false)
    private int communicationScore;

    @Column(name = "japanese_score", nullable = false)
    private int japaneseScore;

    @Column(name = "technical_score", nullable = false)
    private int technicalScore;

    @Column(name = "confidence_score", nullable = false)
    private int confidenceScore;

    @Column(name = "radar_json", columnDefinition = "LONGTEXT")
    private String radarJson;

    @Column(name = "strong_points_json", columnDefinition = "LONGTEXT")
    private String strongPointsJson;

    @Column(name = "weak_points_json", columnDefinition = "LONGTEXT")
    private String weakPointsJson;

    @Column(name = "feedback", columnDefinition = "LONGTEXT")
    private String feedback;

    @Column(name = "star_json", columnDefinition = "LONGTEXT")
    private String starJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
