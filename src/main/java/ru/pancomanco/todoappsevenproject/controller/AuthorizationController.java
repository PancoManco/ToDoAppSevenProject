package ru.pancomanco.todoappsevenproject.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.pancomanco.todoappsevenproject.dto.AuthResponse;
import ru.pancomanco.todoappsevenproject.dto.TokenPair;
import ru.pancomanco.todoappsevenproject.dto.request.*;
import ru.pancomanco.todoappsevenproject.dto.response.MessageResponseDto;
import ru.pancomanco.todoappsevenproject.dto.response.RegisterResponseDto;
import ru.pancomanco.todoappsevenproject.properties.AuthProperties;
import ru.pancomanco.todoappsevenproject.service.AuthenticationService;
import ru.pancomanco.todoappsevenproject.service.EmailVerificationService;
import ru.pancomanco.todoappsevenproject.service.MessageService;
import ru.pancomanco.todoappsevenproject.service.PasswordResetService;
import ru.pancomanco.todoappsevenproject.util.RefreshCookieHelper;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthenticationService authService;
    private final RefreshCookieHelper refreshCookieHelper;
    private final AuthProperties authProperties;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final MessageService messageService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        authService.register(request);

        return ResponseEntity.accepted()
                .body(new RegisterResponseDto(
                        messageService.get("auth.register.verification_sent"),
                        request.email()
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
    @RateLimiter(name = "authLimiter")
    public ResponseEntity<MessageResponseDto> resendVerificationCode(
            @Valid @RequestBody ResendEmailVerificationRequestDto request
    ) {
        emailVerificationService.resendCode(request.email());

        return ResponseEntity.ok()
                .body(new MessageResponseDto(messageService.get("auth.verification.resend_code_sent")));
    }
    @PostMapping("/login")
    @RateLimiter(name = "authLimiter")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequestDto request) {
        TokenPair tokens = authService.login(request);
        return authResponse(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshCookieHelper.NAME) String refreshToken
    ) {

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
    @PostMapping("/forgot-password")
    @RateLimiter(name = "authLimiter")
    public ResponseEntity<MessageResponseDto> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request
    ) {
        passwordResetService.sendResetLink(request.email());

        return ResponseEntity.ok()
                .body(new MessageResponseDto(
                        messageService.get("auth.password.reset_link_sent")
                ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDto> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request
    ) {
        passwordResetService.resetPassword(
                request.token(),
                request.newPassword()
        );

        return ResponseEntity.ok()
                .body(new MessageResponseDto(messageService.get("auth.password.reset_success")));
    }
}
