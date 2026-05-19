package ru.pancomanco.todoappsevenproject.util;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class RefreshCookieHelper {
    public static final String NAME = "refresh_token";
    public static final String PATH = "/api/v1/auth";

    private RefreshCookieHelper() {}

    public static ResponseCookie create(String token, Duration maxAge) {
        return ResponseCookie.from(NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }

    public static ResponseCookie clear() {
        return ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(false) // в локалке false в продакшене true
                .sameSite("Strict")
                .path(PATH)
                .maxAge(0)
                .build();
    }

}
