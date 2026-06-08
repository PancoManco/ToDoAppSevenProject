package ru.pancomanco.todoappsevenproject.service.impl;

import lombok.AllArgsConstructor;
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

import java.util.Locale;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerificationService;

    @Override
    public void register(RegisterRequestDto registerRequestDto) {
        String email = normalizeEmail(registerRequestDto.email());
        String passwordHash = passwordEncoder.encode(registerRequestDto.password());
        String name = normalizeName(registerRequestDto.name());

        Optional<User> existingUserOptional =
                authRepository.findByEmailIgnoreCase(email);

        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();

            if (Boolean.TRUE.equals(existingUser.getEnabled())) {
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
    }

    @Override
    public TokenPair login(LoginRequestDto loginRequestDto) {
        String email = normalizeEmail(loginRequestDto.email());

        User user = authRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.AUTH_INVALID_CREDENTIALS
                ));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new EmailVerificationException(
                    ErrorCode.AUTH_EMAIL_NOT_VERIFIED
            );
        }

        if (!passwordEncoder.matches(
                loginRequestDto.password(),
                user.getPassword()
        )) {
            throw new UnauthorizedException(
                    ErrorCode.AUTH_INVALID_CREDENTIALS
            );
        }

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
        user.setPassword(passwordHash);
        user.setName(name);
        user.setAvatarUrl(null);
        user.setEnabled(false);

        emailVerificationService.sendVerificationCode(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
