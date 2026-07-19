package ru.pancomanco.authservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.authservice.config.EmailSender;
import ru.pancomanco.authservice.dto.TokenPair;
import ru.pancomanco.authservice.entity.EmailVerificationCode;
import ru.pancomanco.authservice.entity.User;
import ru.pancomanco.authservice.messaging.event.UserVerifiedEvent;
import ru.pancomanco.authservice.exception.EmailVerificationException;
import ru.pancomanco.authservice.exception.ErrorCode;
import ru.pancomanco.authservice.repository.AuthRepository;
import ru.pancomanco.authservice.repository.EmailVerificationCodeRepository;
import ru.pancomanco.authservice.service.EmailVerificationService;
import ru.pancomanco.authservice.messaging.outbox.OutboxService;
import ru.pancomanco.authservice.service.TokenService;
import ru.pancomanco.authservice.util.EmailUtil;
import ru.pancomanco.authservice.util.VerificationCodeGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final AuthRepository authRepository;
    private final EmailVerificationCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final TokenService tokenService;
    private final OutboxService outboxService;

    @Override
    public void sendVerificationCode(User user, Locale locale) {

        if (Boolean.TRUE.equals(user.getEnabled())) {
            throw new EmailVerificationException(
                    ErrorCode.AUTH_EMAIL_ALREADY_VERIFIED
            );
        }

        codeRepository.markAllActiveCodesAsUsedByUserId(user.getId(), Instant.now());

        String code = VerificationCodeGenerator.getRandomVerificationCode();
        String codeHash = passwordEncoder.encode(code);

        EmailVerificationCode verificationCode = new EmailVerificationCode(
                user,
                codeHash,
                Instant.now().plus(CODE_TTL)
        );

        codeRepository.save(verificationCode);
        try {
            emailSender.sendVerificationCode(user.getEmail(), code,locale);
            log.info("Verification code sent successfully to email: {}", user.getEmail());
        } catch (MailException ex) {
            log.error("Failed to send verification code to email: {}. Reason: {}", user.getEmail(), ex.getMessage());
            throw new EmailVerificationException(
                    ErrorCode.AUTH_VERIFICATION_EMAIL_SEND_FAILED, ex
            );
        }
    }

    @Override
    @Transactional(noRollbackFor = EmailVerificationException.class)
    public TokenPair verifyEmail(String email, String code) {
        String normalizedEmail = EmailUtil.normalize(email);

        User user = authRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new EmailVerificationException(
                        ErrorCode.AUTH_VERIFICATION_CODE_INVALID
                ));

        if (Boolean.TRUE.equals(user.getEnabled())) {
            log.debug("Attempt to verify already enabled account for email: {}", email);
            throw new EmailVerificationException(
                    ErrorCode.AUTH_EMAIL_ALREADY_VERIFIED
            );
        }

        EmailVerificationCode verificationCode = codeRepository
                .findLatestActiveCodeForUpdate(user.getId())
                .orElseThrow(() -> new EmailVerificationException(
                        ErrorCode.AUTH_VERIFICATION_CODE_INVALID
                ));

        if (verificationCode.isExpired()) {
            verificationCode.markAsUsed();
            throw new EmailVerificationException(
                    ErrorCode.AUTH_VERIFICATION_CODE_EXPIRED
            );
        }

        if (!verificationCode.hasAttemptsLeft()) {
            verificationCode.markAsUsed();
            log.warn("Verification attempts exceeded for email: {}. Account locked from verification.", email);
            throw new EmailVerificationException(
                    ErrorCode.AUTH_VERIFICATION_ATTEMPTS_EXCEEDED
            );
        }

        verificationCode.increaseAttempts();

        if (!passwordEncoder.matches(code, verificationCode.getCodeHash())) {
            log.warn("Invalid verification code attempt for email: {}. Attempts: {}/{}",
                    email, verificationCode.getAttempts(), EmailVerificationCode.MAX_ATTEMPTS);
            throw new EmailVerificationException(
                    ErrorCode.AUTH_VERIFICATION_CODE_INVALID
            );
        }
        verificationCode.markAsUsed();
        user.setEnabled(true);
        log.info("Email successfully verified for user ID: {}, email: {}", user.getId(), email);

        String eventId = UUID.randomUUID().toString();
        UserVerifiedEvent event = new UserVerifiedEvent(
                eventId,
                user.getId(),
                user.getEmail(),
                user.getName(),
                Instant.now()
        );
        outboxService.save(eventId, "UserVerified", "user-events", event);

        return tokenService.issueTokenPair(user);
    }

    @Override
    public void resendCode(String email,Locale locale) {
        String normalizedEmail = EmailUtil.normalize(email);
        Optional<User> userOptional = authRepository.findByEmail(normalizedEmail);
        if (userOptional.isEmpty()) {
            log.debug("Resend code requested for non-existent email: {}. Ignored for security.", email);
            return;
        }
        User user = userOptional.get();
        if (Boolean.TRUE.equals(user.getEnabled())) {
            log.debug("Resend code requested for already verified email: {}. Ignored.", email);
            return;
        }
        sendVerificationCode(user,locale);
    }

}
