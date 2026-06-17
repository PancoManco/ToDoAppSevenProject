package ru.pancomanco.todoappsevenproject.scheduler;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import ru.pancomanco.todoappsevenproject.config.TestcontainersConfiguration;
import ru.pancomanco.todoappsevenproject.entity.*;
import ru.pancomanco.todoappsevenproject.repository.*;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
public class AuthCleanupJobIT {
    @Autowired
    private AuthCleanupJob cleanupJob;
    @Autowired private AuthRepository authRepository;
    @Autowired private EmailVerificationCodeRepository codeRepository;
    @Autowired private PasswordResetTokenRepository resetTokenRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private LinkedAccountRepository linkedAccountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void cleanup_ShouldDeleteOldZombiesButKeepFreshAndOAuth() {

        User oldUser = new User("old-user@test.com", passwordEncoder.encode("password"));
        oldUser.setEnabled(false);
        oldUser.setCreatedAt(Instant.now().minus(Duration.ofDays(2)));
        authRepository.save(oldUser);


        User freshZombie = new User("fresh-user@test.com", passwordEncoder.encode("password"));
        freshZombie.setEnabled(false);
        authRepository.save(freshZombie);


        User oauthZombie = new User("oauth-user@test.com", null);
        oauthZombie.setEnabled(false);
        oauthZombie.setCreatedAt(Instant.now().minus(Duration.ofDays(2)));
        authRepository.save(oauthZombie);

        linkedAccountRepository.save(new LinkedAccount(
                oauthZombie, AuthProviderEnum.GOOGLE, "google-id-123", "oauth-user@test.com"
        ));

        cleanupJob.cleanupAuthData();

        assertThat(authRepository.findByEmail("old-user@test.com")).isEmpty();
        assertThat(authRepository.findByEmail("fresh-user@test.com")).isPresent();
        assertThat(authRepository.findByEmail("oauth-user@test.com")).isPresent();
    }

    @Test
    void cleanup_ShouldDeleteUsedCodesAndTokensOlderThanOneHour() {

        User user = new User("user@test.com", passwordEncoder.encode("password"));
        user.setEnabled(true);
        authRepository.save(user);


        EmailVerificationCode usedCode = new EmailVerificationCode(
                user, "hash", Instant.now().plus(Duration.ofMinutes(5))
        );
        usedCode.markAsUsed();

        setUsedAtViaReflection(usedCode, Instant.now().minus(Duration.ofHours(2)));
        codeRepository.save(usedCode);

        EmailVerificationCode activeCode = new EmailVerificationCode(
                user, "hash2", Instant.now().plus(Duration.ofMinutes(5))
        );
        codeRepository.save(activeCode);

        cleanupJob.cleanupAuthData();

        assertThat(codeRepository.findAll()).hasSize(1);
        assertThat(codeRepository.findAll().get(0).getCodeHash()).isEqualTo("hash2");
    }

    @Test
    void cleanup_ShouldDeleteExpiredAndOldRevokedRefreshTokens() {
        User user = new User("user2@test.com", passwordEncoder.encode("password"));
        user.setEnabled(true);
        authRepository.save(user);

        RefreshToken expiredToken = new RefreshToken(
                user, "hash-expired", Instant.now().minus(Duration.ofDays(1))
        );
        refreshTokenRepository.save(expiredToken);

        RefreshToken oldRevokedToken = new RefreshToken(
                user, "hash-old-revoked", Instant.now().plus(Duration.ofDays(7))
        );
        oldRevokedToken.revoke();
        oldRevokedToken.setCreatedAt(Instant.now().minus(Duration.ofDays(2)));
        refreshTokenRepository.save(oldRevokedToken);

        RefreshToken freshRevokedToken = new RefreshToken(
                user, "hash-fresh-revoked", Instant.now().plus(Duration.ofDays(7))
        );
        freshRevokedToken.revoke();
        refreshTokenRepository.save(freshRevokedToken);

        cleanupJob.cleanupAuthData();

        assertThat(refreshTokenRepository.findAll()).hasSize(1);
        assertThat(refreshTokenRepository.findAll().get(0).getTokenHash()).isEqualTo("hash-fresh-revoked");
    }

    @Test
    void cronExpression_ShouldRunExactlyEveryHourAtMinuteZero() {
        String cron = "0 0 * * * *";

        CronExpression expression = CronExpression.parse(cron);

        Instant now = Instant.parse("2026-06-17T10:30:00Z");
        Instant nextRun = expression.next(now);

        assertThat(nextRun).isEqualTo(Instant.parse("2026-06-17T11:00:00Z"));
    }


    private void setUsedAtViaReflection(EmailVerificationCode code, Instant time) {
        try {
            var field = EmailVerificationCode.class.getDeclaredField("usedAt");
            field.setAccessible(true);
            field.set(code, time);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set usedAt via reflection", e);
        }
    }
}

