package ru.pancomanco.todoappsevenproject.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.config.EmailSender;
import ru.pancomanco.todoappsevenproject.entity.PasswordResetToken;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.exception.ErrorCode;
import ru.pancomanco.todoappsevenproject.exception.PasswordResetException;
import ru.pancomanco.todoappsevenproject.properties.AuthProperties;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import ru.pancomanco.todoappsevenproject.repository.PasswordResetTokenRepository;
import ru.pancomanco.todoappsevenproject.repository.RefreshTokenRepository;
import ru.pancomanco.todoappsevenproject.service.PasswordResetService;
import ru.pancomanco.todoappsevenproject.service.TokenService;
import ru.pancomanco.todoappsevenproject.util.EmailUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(15);

    private final AuthRepository authRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final AuthProperties properties;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void sendResetLink(String email) {
        String normalizedEmail = EmailUtil.normalize(email);

        Optional<User> userOptional =
                authRepository.findByEmail(normalizedEmail);

        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            return;
        }

        resetTokenRepository.markAllActiveTokensAsUsedByUserId(
                user.getId(),
                Instant.now()
        );

        String rawToken = generateResetToken();
        String tokenHash = tokenService.sha256(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken(
                user,
                tokenHash,
                Instant.now().plus(RESET_TOKEN_TTL)
        );

        resetTokenRepository.save(resetToken);

        String resetLink = properties.frontendOrigin()
                           + "/reset-password?token="
                           + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

        try {
            emailSender.sendPasswordResetLink(user.getEmail(), resetLink);
        } catch (MailException ex) {
            throw new PasswordResetException(
                    ErrorCode.AUTH_PASSWORD_RESET_EMAIL_SEND_FAILED,
                    ex
            );
        }
    }

    @Override
    @Transactional(noRollbackFor = PasswordResetException.class)
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new PasswordResetException(
                    ErrorCode.AUTH_PASSWORD_RESET_TOKEN_INVALID
            );
        }
        String tokenHash = tokenService.sha256(token);

        PasswordResetToken resetToken = resetTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new PasswordResetException(
                        ErrorCode.AUTH_PASSWORD_RESET_TOKEN_INVALID
                ));

        if (resetToken.isUsed()) {
            throw new PasswordResetException(
                    ErrorCode.AUTH_PASSWORD_RESET_TOKEN_INVALID
            );
        }

        if (resetToken.isExpired()) {
            resetToken.markAsUsed();

            throw new PasswordResetException(
                    ErrorCode.AUTH_PASSWORD_RESET_TOKEN_EXPIRED
            );
        }

        User user = resetToken.getUser();

        resetToken.markAsUsed();

        user.setPassword(passwordEncoder.encode(newPassword));

        refreshTokenRepository.revokeAllActiveTokensByUserId(user.getId());
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
