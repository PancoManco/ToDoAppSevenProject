package ru.pancomanco.authservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.authservice.repository.AuthRepository;
import ru.pancomanco.authservice.repository.EmailVerificationCodeRepository;
import ru.pancomanco.authservice.repository.PasswordResetTokenRepository;
import ru.pancomanco.authservice.repository.RefreshTokenRepository;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthCleanupJob {

    private static final Duration UNVERIFIED_USER_TTL = Duration.ofHours(24);
    private static final Duration USED_VERIFICATION_CODE_RETENTION = Duration.ofHours(1);
    private static final Duration USED_PASSWORD_RESET_TOKEN_RETENTION = Duration.ofHours(1);
    private static final Duration REVOKED_REFRESH_TOKEN_RETENTION = Duration.ofDays(1);

    private final AuthRepository authRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupAuthData() {
        log.debug("Starting scheduled auth cleanup job...");
        Instant now = Instant.now();

        Instant unverifiedUserCutoff = now.minus(UNVERIFIED_USER_TTL);
        Instant usedVerificationCodeCutoff = now.minus(USED_VERIFICATION_CODE_RETENTION);
        Instant usedPasswordResetTokenCutoff = now.minus(USED_PASSWORD_RESET_TOKEN_RETENTION);
        Instant revokedRefreshTokenCutoff = now.minus(REVOKED_REFRESH_TOKEN_RETENTION);

        int deletedExpiredOrUsedVerificationCodes =
                emailVerificationCodeRepository.deleteExpiredOrUsedBefore(
                        now,
                        usedVerificationCodeCutoff
                );

        int deletedPasswordResetTokens =
                passwordResetTokenRepository.deleteExpiredOrUsedBefore(
                        now,
                        usedPasswordResetTokenCutoff
                );

        int deletedRefreshTokens =
                refreshTokenRepository.deleteExpiredOrRevokedBefore(
                        now,
                        revokedRefreshTokenCutoff
                );

        int deletedCodesForZombieUsers =
                emailVerificationCodeRepository.deleteCodesForUnverifiedUsersCreatedBefore(
                        unverifiedUserCutoff
                );

        int deletedZombieUsers =
                authRepository.deleteUnverifiedUsersCreatedBefore(
                        unverifiedUserCutoff
                );

        log.info(
                """
                Auth cleanup completed:
                deletedExpiredOrUsedVerificationCodes={}
                deletedPasswordResetTokens={}
                deletedRefreshTokens={}
                deletedCodesForZombieUsers={}
                deletedZombieUsers={}
                """,
                deletedExpiredOrUsedVerificationCodes,
                deletedPasswordResetTokens,
                deletedRefreshTokens,
                deletedCodesForZombieUsers,
                deletedZombieUsers
        );
    }
}
