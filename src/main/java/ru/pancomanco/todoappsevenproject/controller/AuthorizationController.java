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
import ru.pancomanco.todoappsevenproject.service.AuthenticationService;
import ru.pancomanco.todoappsevenproject.util.RefreshCookieHelper;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthorizationController {

    private static final Duration REFRESH_COOKIE_MAX_AGE = Duration.ofDays(7);
    private final AuthenticationService authService;
    private final RefreshCookieHelper refreshCookieHelper;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequestDto request) {
        TokenPair tokens = authService.register(request);
        return authResponse(tokens);
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
                REFRESH_COOKIE_MAX_AGE
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new AuthResponse(tokens.accessToken()));
    }
}
