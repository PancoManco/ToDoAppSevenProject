package ru.pancomanco.todoappsevenproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "email_verification_codes",
        indexes = {
                @Index(name = "idx_email_verification_user_id", columnList = "user_id"),
                @Index(name = "idx_email_verification_expires_at", columnList = "expiresAt")
        })
@Getter
@NoArgsConstructor
public class EmailVerificationCode {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private int attempts = 0;

    @Column
    private Instant usedAt;

    public EmailVerificationCode(User user, String codeHash, Instant expiresAt) {
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean hasAttemptsLeft() {
        return attempts < MAX_ATTEMPTS;
    }

    public void increaseAttempts() {
        this.attempts++;
    }

    public void markAsUsed() {
        this.usedAt = Instant.now();
    }
}
