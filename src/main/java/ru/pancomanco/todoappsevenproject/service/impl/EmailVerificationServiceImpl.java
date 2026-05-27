package ru.pancomanco.todoappsevenproject.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.config.EmailSender;
import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.entity.EmailVerificationCode;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.exception.UnauthorizedException;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import ru.pancomanco.todoappsevenproject.repository.EmailVerificationCodeRepository;
import ru.pancomanco.todoappsevenproject.service.EmailVerificationService;
import ru.pancomanco.todoappsevenproject.service.TokenService;
import ru.pancomanco.todoappsevenproject.util.VerificationCodeGenerator;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

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

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void sendVerificationCode(User user) {
        codeRepository.markUsedAllActiveCodesByUserId(user.getId(), Instant.now());

        String code = VerificationCodeGenerator.getRandomVerificationCode();
        String codeHash = passwordEncoder.encode(code);

        EmailVerificationCode verificationCode = new EmailVerificationCode(
                user,
                codeHash,
                Instant.now().plus(CODE_TTL)
        );

        codeRepository.save(verificationCode);

        emailSender.sendVerificationCode(user.getEmail(), code);
    }

    @Override
    public TokenPair verifyEmail(String email, String code) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = authRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid verification code"));

        EmailVerificationCode verificationCode = codeRepository
                .findLatestActiveCodeForUpdate(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Invalid verification code"));

        if (verificationCode.isExpired()) {
            verificationCode.markAsUsed();
            throw new UnauthorizedException("Verification code expired");
        }

        if (!verificationCode.hasAttemptsLeft()) {
            verificationCode.markAsUsed();
            throw new UnauthorizedException("Too many verification attempts");
        }

        verificationCode.increaseAttempts();

        if (!passwordEncoder.matches(code, verificationCode.getCodeHash())) {
            throw new UnauthorizedException("Invalid verification code");
        }

        verificationCode.markAsUsed();
        user.setEnabled(true);

        return tokenService.issueTokenPair(user);
    }

    @Override
    public void resendCode(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = authRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (Boolean.TRUE.equals(user.getEnabled())) {
            return;
        }

        sendVerificationCode(user);
    }

    private String generateSixDigitCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }
}
