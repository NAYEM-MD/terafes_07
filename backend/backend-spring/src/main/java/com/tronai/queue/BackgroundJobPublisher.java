package com.tronai.queue;

public interface BackgroundJobPublisher {

    /**
     * Publish CV processed event asynchronously.
     */
    void publishCvProcessed(Long userId, Long cvId, String filename);

    /**
     * Publish Interview completed event asynchronously.
     */
    void publishInterviewCompleted(Long userId, String interviewId, int overallScore);

    /**
     * Publish generic TronEvent.
     */
    void publishEvent(String routingKey, TronEvent event);

    /**
     * Health check for RabbitMQ connectivity.
     */
    boolean isAvailable();
}
