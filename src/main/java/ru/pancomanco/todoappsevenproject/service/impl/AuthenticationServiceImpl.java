package ru.pancomanco.todoappsevenproject.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.dto.request.LoginRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.RegisterRequestDto;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.exception.EmailAlreadyExistsException;
import ru.pancomanco.todoappsevenproject.exception.UnauthorizedException;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import ru.pancomanco.todoappsevenproject.service.AuthenticationService;
import ru.pancomanco.todoappsevenproject.service.EmailVerificationService;
import ru.pancomanco.todoappsevenproject.service.TokenService;

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
        String email = registerRequestDto.email().trim().toLowerCase();

        if (authRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        String passwordHash = passwordEncoder.encode(registerRequestDto.password());

        User user = new User(email, passwordHash);
        user.setName(registerRequestDto.name().trim());
        user.setEnabled(false);

        authRepository.save(user);

        emailVerificationService.sendVerificationCode(user);
    }

    @Override
    public TokenPair login(LoginRequestDto loginRequestDto) {
        User user = authRepository.findByEmailIgnoreCase(loginRequestDto.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new UnauthorizedException("Email is not verified");
        }

        if (!passwordEncoder.matches(loginRequestDto.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
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
}
