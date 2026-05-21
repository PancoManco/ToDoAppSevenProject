package ru.pancomanco.todoappsevenproject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.pancomanco.todoappsevenproject.dto.AuthResponse;
import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.dto.request.LoginRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.RegisterRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.ResendEmailVerificationRequestDto;
import ru.pancomanco.todoappsevenproject.dto.request.VerifyEmailRequestDto;
import ru.pancomanco.todoappsevenproject.properties.AuthProperties;
import ru.pancomanco.todoappsevenproject.service.AuthenticationService;
import ru.pancomanco.todoappsevenproject.service.EmailVerificationService;
import ru.pancomanco.todoappsevenproject.util.RefreshCookieHelper;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthorizationController {

    private final AuthenticationService authService;
    private final RefreshCookieHelper refreshCookieHelper;
    private final AuthProperties authProperties;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDto request) {
        authService.register(request);

        return ResponseEntity.accepted()
                .body(Map.of(
                        "message", "Verification code sent to email",
                        "email", request.email()
                ));
    }
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequestDto request
    ) {
        TokenPair tokens = emailVerificationService.verifyEmail(
                request.email(),
                request.code()
        );

        return authResponse(tokens);
    }

    @PostMapping("/resend-verification-code")
    public ResponseEntity<?> resendVerificationCode(
            @Valid @RequestBody ResendEmailVerificationRequestDto request
    ) {
        emailVerificationService.resendCode(request.email());

        return ResponseEntity.ok()
                .body(Map.of("message", "Verification code sent"));
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequestDto request) {
        TokenPair tokens = authService.login(request);
        return authResponse(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshCookieHelper.NAME) String refreshToken
    ) {
        log.info("Refresh token: {}", refreshToken);

        TokenPair tokens = authService.refresh(refreshToken);
        return authResponse(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookieHelper.NAME, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieHelper.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> authResponse(TokenPair tokens) {
        ResponseCookie refreshCookie = refreshCookieHelper.create(
                tokens.refreshToken(),
                Duration.ofDays(authProperties.jwt().refreshTokenDays())
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new AuthResponse(tokens.accessToken()));
    }
}
