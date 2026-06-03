package ru.pancomanco.todoappsevenproject.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.config.EmailSender;
import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.entity.EmailVerificationCode;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.exception.EmailVerificationException;
import ru.pancomanco.todoappsevenproject.exception.ErrorCode;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import ru.pancomanco.todoappsevenproject.repository.EmailVerificationCodeRepository;
import ru.pancomanco.todoappsevenproject.service.EmailVerificationService;
import ru.pancomanco.todoappsevenproject.service.TokenService;
import ru.pancomanco.todoappsevenproject.util.VerificationCodeGenerator;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final AuthRepository authRepository;
    private final EmailVerificationCodeRepository codeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final TokenService tokenService;


    @Override
    public void sendVerificationCode(User user) {

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
            emailSender.sendVerificationCode(user.getEmail(), code);
        } catch (MailException ex) {
            throw new EmailVerificationException(
                    ErrorCode.AUTH_VERIFICATION_EMAIL_SEND_FAILED, ex
            );
        }
    }

    @Override
    @Transactional(noRollbackFor = EmailVerificationException.class)
    public TokenPair verifyEmail(String email, String code) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = authRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EmailVerificationException(
                        ErrorCode.AUTH_VERIFICATION_CODE_INVALID
                ));

        if (Boolean.TRUE.equals(user.getEnabled())) {
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
            throw new EmailVerificationException(
                    ErrorCode.AUTH_VERIFICATION_ATTEMPTS_EXCEEDED
            );
        }

        verificationCode.increaseAttempts();

        if (!passwordEncoder.matches(code, verificationCode.getCodeHash())) {
            throw new EmailVerificationException(
                    ErrorCode.AUTH_VERIFICATION_CODE_INVALID
            );
        }
        verificationCode.markAsUsed();
        user.setEnabled(true);

        return tokenService.issueTokenPair(user);
    }

    @Override
    public void resendCode(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOptional = authRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOptional.isEmpty()) {
            return;
        }
        User user = userOptional.get();
        if (Boolean.TRUE.equals(user.getEnabled())) {
            return;
        }
        sendVerificationCode(user);
    }

}
