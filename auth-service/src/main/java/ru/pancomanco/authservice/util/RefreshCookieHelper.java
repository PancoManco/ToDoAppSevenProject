package ru.pancomanco.authservice.util;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import ru.pancomanco.authservice.properties.AuthProperties;

import java.time.Duration;

@Component
public final class RefreshCookieHelper {
    public static final String NAME = "refresh_token";
    public static final String PATH = "/api/v1/auth";

    private final AuthProperties properties;

    public RefreshCookieHelper(AuthProperties properties) {
        this.properties = properties;
    }


    public ResponseCookie create(String token, Duration maxAge) {
        return ResponseCookie.from(NAME, token)
                .httpOnly(true)
                .secure(properties.cookie().secure())
                .sameSite(properties.cookie().sameSite())
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(properties.cookie().secure())
                .sameSite(properties.cookie().sameSite())
                .path(PATH)
                .maxAge(0)
                .build();
    }

}
