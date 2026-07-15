package ru.pancomanco.todoappsevenproject.messaging.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "outbox")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    private static final int MAX_ATTEMPTS = 10;
    private static final int MAX_LAST_ERROR_LENGTH = 1000;
    private static final long MAX_BACKOFF_SECONDS = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(nullable = false, updatable = false)
    private String topic;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private boolean dead = false;

    public OutboxEvent(String eventId, String eventType, String topic, String payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();

        this.createdAt = now;

        if (this.nextAttemptAt == null) {
            this.nextAttemptAt = now;
        }
    }

    public void markPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
        this.lastError = null;
        this.dead = false;
    }

    public void markFailed(Throwable error) {
        this.attempts++;

        this.lastError = extractErrorMessage(error);

        if (this.attempts >= MAX_ATTEMPTS) {
            this.dead = true;
            return;
        }

        long delaySeconds = calculateBackoffSeconds(this.attempts);
        this.nextAttemptAt = Instant.now().plusSeconds(delaySeconds);
    }

    private long calculateBackoffSeconds(int attempts) {
        long delay = (long) Math.pow(2, attempts);
        return Math.min(delay, MAX_BACKOFF_SECONDS);
    }

    private String extractErrorMessage(Throwable error) {
        if (error == null) {
            return null;
        }

        String message = error.getMessage();

        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }

        if (message.length() > MAX_LAST_ERROR_LENGTH) {
            return message.substring(0, MAX_LAST_ERROR_LENGTH);
        }

        return message;
    }
}
