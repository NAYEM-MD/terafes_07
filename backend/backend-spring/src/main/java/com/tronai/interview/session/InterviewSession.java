package com.tronai.interview.session;

import com.tronai.interview.dto.InterviewConfigDto;
import com.tronai.interview.dto.InterviewQuestionDto;
import com.tronai.interview.dto.TranscriptMessageDto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe state container for an active or completed interview session.
 * Holds immutable locked AI Profile and actual model ID.
 */
public class InterviewSession {

    private final String interviewId;
    private final InterviewConfigDto config;
    private final int questionCount;
    private final Instant startedAt;
    private final String aiProfile;
    private final String modelId;
    private final ReentrantLock lock = new ReentrantLock();

    private int currentQuestionIndex;
    private boolean finished;
    private Instant endedAt;

    private final List<InterviewQuestionDto> questions = new ArrayList<>();
    private final List<TranscriptMessageDto> transcript = new ArrayList<>();
    private final List<InterviewTurn> turns = new ArrayList<>();
    private final List<String> askedQuestions = new ArrayList<>();
    private final List<String> coveredTopics = new ArrayList<>();

    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault());

    public InterviewSession(String interviewId, InterviewConfigDto config, int questionCount) {
        this(interviewId, config, questionCount,
                (config != null && config.getAiProfile() != null && !config.getAiProfile().isBlank()) ? config.getAiProfile() : "FAST",
                "tron-fast");
    }

    public InterviewSession(String interviewId, InterviewConfigDto config, int questionCount, String aiProfile, String modelId) {
        this.interviewId = interviewId;
        this.config = config;
        this.questionCount = questionCount;
        this.startedAt = Instant.now();
        this.currentQuestionIndex = 0;
        this.finished = false;
        this.aiProfile = (aiProfile != null && !aiProfile.isBlank()) ? aiProfile.trim().toUpperCase() : "FAST";
        this.modelId = modelId != null ? modelId : "tron-fast";
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public InterviewConfigDto getConfig() {
        return config;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public String getAiProfile() {
        return aiProfile;
    }

    public String getModelId() {
        return modelId;
    }

    public synchronized int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public synchronized void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized void setFinished(boolean finished) {
        this.finished = finished;
        if (finished && this.endedAt == null) {
            this.endedAt = Instant.now();
        }
    }

    public synchronized Instant getEndedAt() {
        return endedAt;
    }

    public synchronized List<InterviewQuestionDto> getQuestions() {
        return new ArrayList<>(questions);
    }

    public synchronized void setQuestions(List<InterviewQuestionDto> newQuestions) {
        this.questions.clear();
        if (newQuestions != null) {
            this.questions.addAll(newQuestions);
        }
    }

    public synchronized List<TranscriptMessageDto> getTranscript() {
        return new ArrayList<>(transcript);
    }

    public synchronized void addTranscriptMessage(TranscriptMessageDto message) {
        this.transcript.add(message);
    }

    public synchronized List<InterviewTurn> getTurns() {
        return new ArrayList<>(turns);
    }

    public synchronized void addTurn(String role, String content) {
        this.turns.add(new InterviewTurn(role, content));
    }

    public synchronized List<String> getAskedQuestions() {
        return new ArrayList<>(askedQuestions);
    }

    public synchronized void recordAskedQuestion(String question, String topic) {
        if (question != null && !question.isBlank()) {
            this.askedQuestions.add(question);
        }
        if (topic != null && !topic.isBlank() && !this.coveredTopics.contains(topic)) {
            this.coveredTopics.add(topic);
        }
    }

    public synchronized List<String> getCoveredTopics() {
        return new ArrayList<>(coveredTopics);
    }

    public static String currentClock() {
        return CLOCK_FORMAT.format(Instant.now());
    }
}
