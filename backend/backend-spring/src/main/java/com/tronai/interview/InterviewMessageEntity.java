package com.tronai.interview;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "interview_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSessionEntity session;

    @Column(name = "role", nullable = false, length = 30)
    private String role;

    @Column(name = "question_index", nullable = false)
    private int questionIndex;

    @Column(name = "message_type", nullable = false, length = 50)
    @Builder.Default
    private String messageType = "text";

    @Column(name = "text", nullable = false, columnDefinition = "LONGTEXT")
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
