package ru.pancomanco.todoappsevenproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "email_verification_codes",
        indexes = {
                @Index(name = "idx_email_verification_user_id", columnList = "user_id"),
                @Index(name = "idx_email_verification_expires_at", columnList = "expires_at")
        })
@Getter
@NoArgsConstructor
public class EmailVerificationCode {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "used_at")
    private Instant usedAt;

    public EmailVerificationCode(User user, String codeHash, Instant expiresAt) {
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
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
