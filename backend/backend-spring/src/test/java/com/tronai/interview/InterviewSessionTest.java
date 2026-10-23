package com.tronai.interview;

import com.tronai.interview.dto.InterviewConfigDto;
import com.tronai.interview.session.InterviewSession;
import com.tronai.interview.session.InterviewSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewSessionTest {

    private InterviewSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new InterviewSessionManager();
    }

    @Test
    @DisplayName("Session manager creates and retrieves unique sessions")
    void createAndGetSession() {
        InterviewConfigDto config = new InterviewConfigDto();
        config.setCompanyName("LINEヤフー");
        config.setQuestionCount(5);

        InterviewSession session = sessionManager.createSession(config, 5);

        assertThat(session.getInterviewId()).isNotBlank();
        assertThat(session.getQuestionCount()).isEqualTo(5);
        assertThat(session.getCurrentQuestionIndex()).isEqualTo(0);
        assertThat(session.isFinished()).isFalse();

        InterviewSession retrieved = sessionManager.getSession(session.getInterviewId());
        assertThat(retrieved).isSameAs(session);
    }

    @Test
    @DisplayName("Getting non-existent session throws 404 InterviewException")
    void nonExistentSessionThrows404() {
        assertThatThrownBy(() -> sessionManager.getSession("non-existent-uuid"))
                .isInstanceOf(InterviewException.class)
                .satisfies(ex -> {
                    InterviewException ie = (InterviewException) ex;
                    assertThat(ie.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ie.getMessage()).isEqualTo("Interview session was not found");
                });
    }

    @Test
    @DisplayName("Concurrent sessions do not interfere with each other")
    void concurrentSessionsIsolation() throws InterruptedException {
        int sessionCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(sessionCount);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < sessionCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    InterviewConfigDto config = new InterviewConfigDto();
                    config.setCompanyName("Company-" + index);
                    config.setQuestionCount(3);
                    InterviewSession session = sessionManager.createSession(config, 3);
                    session.setCurrentQuestionIndex(index);
                    if (sessionManager.getSession(session.getInterviewId()).getCurrentQuestionIndex() == index) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(sessionCount);
        assertThat(sessionManager.getActiveCount()).isEqualTo(sessionCount);
    }
}
