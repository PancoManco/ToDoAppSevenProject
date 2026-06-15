package ru.pancomanco.todoappsevenproject.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.dto.request.LoginRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.RegisterRequestDto;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.exception.EmailVerificationException;
import ru.pancomanco.todoappsevenproject.exception.ErrorCode;
import ru.pancomanco.todoappsevenproject.exception.UnauthorizedException;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import ru.pancomanco.todoappsevenproject.service.AuthenticationService;
import ru.pancomanco.todoappsevenproject.service.EmailVerificationService;
import ru.pancomanco.todoappsevenproject.service.TokenService;
import ru.pancomanco.todoappsevenproject.util.EmailUtil;

import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerificationService;

    @Override
    public void register(RegisterRequestDto registerRequestDto) {
        String email = EmailUtil.normalize(registerRequestDto.email());
        String passwordHash = passwordEncoder.encode(registerRequestDto.password());
        String name = normalizeName(registerRequestDto.name());

        Optional<User> existingUserOptional =
                authRepository.findByEmail(email);

        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();

            if (Boolean.TRUE.equals(existingUser.getEnabled())) {
                log.warn("Registration attempt with already existing email: {}", email);
                throw new EmailVerificationException(
                        ErrorCode.AUTH_EMAIL_ALREADY_EXISTS
                );
            }
            restartUnverifiedRegistration(
                    existingUser,
                    passwordHash,
                    name
            );
            return;
        }
        User user = new User(email, passwordHash);
        user.setName(name);
        user.setEnabled(false);
        authRepository.save(user);
        emailVerificationService.sendVerificationCode(user);
        log.info("Successful registration for user ID: {}, email: {}", user.getId(), email);
    }

    @Override
    public TokenPair login(LoginRequestDto loginRequestDto) {
        String email = EmailUtil.normalize(loginRequestDto.email());
        User user = authRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("Login attempt with unregistered email: {}", email);
                    return new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
                });
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            log.warn("Login attempt with unverified email: {}", email);
            throw new EmailVerificationException(
                    ErrorCode.AUTH_EMAIL_NOT_VERIFIED
            );
        }
        if (!passwordEncoder.matches(
                loginRequestDto.password(),
                user.getPassword()
        )) {
            log.warn("Failed login attempt (invalid password) for email: {}", email);
            throw new UnauthorizedException(
                    ErrorCode.AUTH_INVALID_CREDENTIALS
            );
        }
        log.info("Successful login for user ID: {}, email: {}", user.getId(), email);
        return tokenService.issueTokenPair(user);
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        return tokenService.rotateRefreshTokenPair(refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        tokenService.revokeRefreshTokenPair(refreshToken);
    }

    private void restartUnverifiedRegistration(
            User user,
            String passwordHash,
            String name
    ) {
        log.info("Starting UnverifiedRegistration ...");
        user.setPassword(passwordHash);
        user.setName(name);
        user.setAvatarUrl(null);
        user.setEnabled(false);

        emailVerificationService.sendVerificationCode(user);
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
