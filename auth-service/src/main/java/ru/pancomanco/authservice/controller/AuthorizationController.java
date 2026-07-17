package ru.pancomanco.authservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.pancomanco.common.i18n.MessageService;
import ru.pancomanco.authservice.config.ClientIpResolver;
import ru.pancomanco.authservice.dto.AuthResponse;
import ru.pancomanco.authservice.dto.TokenPair;
import ru.pancomanco.authservice.dto.request.*;
import ru.pancomanco.authservice.dto.response.MessageResponseDto;
import ru.pancomanco.authservice.dto.response.RegisterResponseDto;
import ru.pancomanco.authservice.properties.AuthProperties;

import ru.pancomanco.authservice.service.AuthenticationService;
import ru.pancomanco.authservice.service.EmailVerificationService;
import ru.pancomanco.authservice.service.PasswordResetService;
import ru.pancomanco.authservice.service.RateLimitService;
import ru.pancomanco.authservice.util.RefreshCookieHelper;

import java.time.Duration;

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
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;


    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(
            @Valid @RequestBody RegisterRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String ip = clientIpResolver.resolve(httpRequest);
        rateLimitService.checkRegister(ip, request.email());

        authService.register(request);

        return ResponseEntity.ok()
                .body(new RegisterResponseDto(
                        messageService.get("auth.register.verification_sent"),
                        request.email()
                ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String ip = clientIpResolver.resolve(httpRequest);
        rateLimitService.checkVerifyEmail(ip, request.email());

        TokenPair tokens = emailVerificationService.verifyEmail(
                request.email(),
                request.code()
        );

        return authResponse(tokens);
    }

    @PostMapping("/resend-verification-code")
    public ResponseEntity<MessageResponseDto> resendVerificationCode(
            @Valid @RequestBody ResendEmailVerificationRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String ip = clientIpResolver.resolve(httpRequest);
        rateLimitService.checkResendVerification(ip, request.email());

        emailVerificationService.resendCode(request.email());

        return ResponseEntity.ok()
                .body(new MessageResponseDto(messageService.get("auth.verification.resend_code_sent")));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String ip = clientIpResolver.resolve(httpRequest);
        rateLimitService.checkLogin(ip, request.email());

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
    public ResponseEntity<MessageResponseDto> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String ip = clientIpResolver.resolve(httpRequest);
        rateLimitService.checkForgotPassword(ip, request.email());

        passwordResetService.sendResetLink(request.email());

        return ResponseEntity.ok()
                .body(new MessageResponseDto(messageService.get("auth.password.reset_link_sent")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDto> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String ip = clientIpResolver.resolve(httpRequest);
        rateLimitService.checkResetPassword(ip, request.token());

        passwordResetService.resetPassword(request.token(), request.newPassword());

        return ResponseEntity.ok()
                .body(new MessageResponseDto(messageService.get("auth.password.reset_success")));
    }


}
