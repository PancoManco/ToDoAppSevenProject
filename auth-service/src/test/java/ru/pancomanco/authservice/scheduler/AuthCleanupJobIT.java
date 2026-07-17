package ru.pancomanco.authservice.scheduler;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.pancomanco.authservice.config.EmailSender;
import ru.pancomanco.authservice.config.TestRateLimitConfig;
import ru.pancomanco.authservice.config.TestcontainersConfiguration;
import ru.pancomanco.authservice.entity.*;
import ru.pancomanco.authservice.repository.*;
import ru.pancomanco.authservice.service.RateLimitService;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Import({TestcontainersConfiguration.class, TestRateLimitConfig.class})
@ActiveProfiles("test")
@Transactional
public class AuthCleanupJobIT {
    @Autowired
    private AuthCleanupJob cleanupJob;
    @Autowired
    private AuthRepository authRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private EmailVerificationCodeRepository codeRepository;
    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;
    @Autowired
    private LinkedAccountRepository linkedAccountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private EmailSender emailSender;
    @MockitoBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        codeRepository.deleteAll();
        resetTokenRepository.deleteAll();
        linkedAccountRepository.deleteAll();
        authRepository.deleteAll();
    }

    private String uniqueEmail() {
        return "cleanup-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private User createVerifiedUser() {
        User user = new User(uniqueEmail(), passwordEncoder.encode("Password123!"));
        user.setName("TestUser");
        user.setEnabled(true);
        return authRepository.save(user);
    }

    private User createUnverifiedUserCreatedAt(Instant createdAt) {
        User user = new User(uniqueEmail(), passwordEncoder.encode("Password123!"));
        user.setName("UnverifiedUser");
        user.setEnabled(false);
        User saved = authRepository.save(user);

        entityManager.createNativeQuery(
                        "UPDATE users SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", saved.getId())
                .executeUpdate();
        entityManager.clear();

        return authRepository.findById(saved.getId()).orElseThrow();
    }

    @Nested
    @DisplayName("Verification codes cleanup")
    class VerificationCodesCleanup {

        @Test
        void cleanup_RemovesExpiredCodes() {
            User user = createVerifiedUser();
            EmailVerificationCode expired = new EmailVerificationCode(
                    user, "hash", Instant.now().minus(Duration.ofMinutes(10)));
            codeRepository.save(expired);

            cleanupJob.cleanupAuthData();

            entityManager.clear();

            assertThat(codeRepository.findById(expired.getId())).isEmpty();
            assertThat(codeRepository.findById(expired.getId())).isEmpty();
        }

        @Test
        void cleanup_KeepsActiveCodes() {
            User user = createVerifiedUser();
            EmailVerificationCode active = new EmailVerificationCode(
                    user, "hash", Instant.now().plus(Duration.ofMinutes(5)));
            codeRepository.save(active);

            cleanupJob.cleanupAuthData();

            assertThat(codeRepository.findById(active.getId())).isPresent();
        }


        @Test
        void cleanup_KeepsRecentlyUsedCodes() {
            User user = createVerifiedUser();
            EmailVerificationCode recent = new EmailVerificationCode(
                    user, "hash", Instant.now().plus(Duration.ofMinutes(5)));
            codeRepository.save(recent);
            recent.markAsUsed();
            codeRepository.save(recent);

            cleanupJob.cleanupAuthData();

            assertThat(codeRepository.findById(recent.getId())).isPresent();
        }
    }

    @Nested
    class PasswordResetTokensCleanup {

        @Test
        void cleanup_RemovesExpiredResetTokens() {
            User user = createVerifiedUser();
            PasswordResetToken expired = new PasswordResetToken(
                    user, "hash", Instant.now().minus(Duration.ofMinutes(10)));
            resetTokenRepository.save(expired);

            cleanupJob.cleanupAuthData();
            entityManager.clear();
            assertThat(resetTokenRepository.findById(expired.getId())).isEmpty();
        }


        @Test
        void cleanup_KeepsActiveResetTokens() {
            User user = createVerifiedUser();
            PasswordResetToken active = new PasswordResetToken(
                    user, "hash", Instant.now().plus(Duration.ofMinutes(10)));
            resetTokenRepository.save(active);

            cleanupJob.cleanupAuthData();

            assertThat(resetTokenRepository.findById(active.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("Refresh tokens cleanup")
    class RefreshTokensCleanup {

        @Test
        void cleanup_RemovesExpiredRefreshTokens() {
            User user = createVerifiedUser();
            RefreshToken expired = new RefreshToken(
                    user, "hash", Instant.now().minus(Duration.ofDays(1)));
            refreshTokenRepository.save(expired);

            cleanupJob.cleanupAuthData();
            entityManager.clear();
            assertThat(refreshTokenRepository.findById(expired.getId())).isEmpty();
        }

        @Test
        void cleanup_RemovesOldRevokedRefreshTokens() {
            User user = createVerifiedUser();
            RefreshToken old = new RefreshToken(
                    user, "hash", Instant.now().plus(Duration.ofDays(7)));
            old.setRevoked(true);
            old.setCreatedAt(Instant.now().minus(Duration.ofDays(2)));
            refreshTokenRepository.save(old);

            cleanupJob.cleanupAuthData();
            entityManager.clear();
            assertThat(refreshTokenRepository.findById(old.getId())).isEmpty();
        }

        @Test
        void cleanup_KeepsActiveRefreshTokens() {
            User user = createVerifiedUser();
            RefreshToken active = new RefreshToken(
                    user, "hash", Instant.now().plus(Duration.ofDays(7)));
            refreshTokenRepository.save(active);

            cleanupJob.cleanupAuthData();

            assertThat(refreshTokenRepository.findById(active.getId())).isPresent();
        }

        @Test
        void cleanup_KeepsRecentlyRevokedTokens() {
            User user = createVerifiedUser();
            RefreshToken recentRevoked = new RefreshToken(
                    user, "hash", Instant.now().plus(Duration.ofDays(7)));
            recentRevoked.setRevoked(true);
            refreshTokenRepository.save(recentRevoked);

            cleanupJob.cleanupAuthData();

            assertThat(refreshTokenRepository.findById(recentRevoked.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("Zombie users cleanup")
    class ZombieUsersCleanup {

        @Test
        void cleanup_RemovesOldUnverifiedUsers() {
            User zombie = createUnverifiedUserCreatedAt(
                    Instant.now().minus(Duration.ofHours(25)));

            cleanupJob.cleanupAuthData();
            entityManager.clear();
            assertThat(authRepository.findById(zombie.getId())).isEmpty();
        }

        @Test
        void cleanup_KeepsRecentUnverifiedUsers() {
            User recent = createUnverifiedUserCreatedAt(
                    Instant.now().minus(Duration.ofHours(1)));

            cleanupJob.cleanupAuthData();
            entityManager.clear();
            assertThat(authRepository.findById(recent.getId())).isPresent();
        }

        @Test
        void cleanup_KeepsVerifiedUsersRegardlessOfAge() {
            User user = createVerifiedUser();
            user.setCreatedAt(Instant.now().minus(Duration.ofDays(365)));
            authRepository.save(user);

            cleanupJob.cleanupAuthData();

            assertThat(authRepository.findById(user.getId())).isPresent();
        }

        @Test
        void cleanup_KeepsUnverifiedUsersWithLinkedAccount() {
            User user = createUnverifiedUserCreatedAt(
                    Instant.now().minus(Duration.ofHours(25)));
            LinkedAccount linked = new LinkedAccount(
                    user, AuthProviderEnum.GOOGLE, "google-id-123", user.getEmail());
            linkedAccountRepository.save(linked);

            cleanupJob.cleanupAuthData();

            assertThat(authRepository.findById(user.getId())).isPresent();
        }

        @Test
        void cleanup_RemovesCodesForZombieUsersBeforeRemovingUsers() {
            User zombie = createUnverifiedUserCreatedAt(
                    Instant.now().minus(Duration.ofHours(25)));
            EmailVerificationCode code = new EmailVerificationCode(
                    zombie, "hash", Instant.now().plus(Duration.ofMinutes(5)));
            codeRepository.save(code);

            cleanupJob.cleanupAuthData();
            entityManager.clear();
            assertThat(codeRepository.findById(code.getId())).isEmpty();
            assertThat(authRepository.findById(zombie.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Combined scenario")
    class CombinedScenario {

        @Test
        void cleanup_HandlesMixedDataInSinglePass() {
            User verifiedUser = createVerifiedUser();
            User zombie = createUnverifiedUserCreatedAt(
                    Instant.now().minus(Duration.ofHours(25)));

            EmailVerificationCode expiredCode = new EmailVerificationCode(
                    verifiedUser, "hash1",
                    Instant.now().minus(Duration.ofMinutes(10)));
            codeRepository.save(expiredCode);

            PasswordResetToken expiredReset = new PasswordResetToken(
                    verifiedUser, "hash2",
                    Instant.now().minus(Duration.ofMinutes(10)));
            resetTokenRepository.save(expiredReset);

            RefreshToken expiredRefresh = new RefreshToken(
                    verifiedUser, "hash3",
                    Instant.now().minus(Duration.ofDays(1)));
            refreshTokenRepository.save(expiredRefresh);

            RefreshToken activeRefresh = new RefreshToken(
                    verifiedUser, "hash4",
                    Instant.now().plus(Duration.ofDays(7)));
            refreshTokenRepository.save(activeRefresh);

            cleanupJob.cleanupAuthData();

            entityManager.clear();

            assertThat(authRepository.findById(verifiedUser.getId())).isPresent();
            assertThat(authRepository.findById(zombie.getId())).isEmpty();
            assertThat(codeRepository.findById(expiredCode.getId())).isEmpty();
            assertThat(resetTokenRepository.findById(expiredReset.getId())).isEmpty();
            assertThat(refreshTokenRepository.findById(expiredRefresh.getId())).isEmpty();
            assertThat(refreshTokenRepository.findById(activeRefresh.getId())).isPresent();
        }
    }
}

