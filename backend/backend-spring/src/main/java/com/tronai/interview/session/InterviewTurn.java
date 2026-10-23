package com.tronai.interview.session;

import java.time.Instant;

/**
 * An individual turn in an interview session.
 */
public class InterviewTurn {

    private final String role; // "interviewer" | "candidate"
    private final String content;
    private final Instant timestamp;

    public InterviewTurn(String role, String content) {
        this.role = role;
        this.content = content != null ? content : "";
        this.timestamp = Instant.now();
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
