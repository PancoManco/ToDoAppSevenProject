package ru.pancomanco.authservice.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "app.security")
public record AuthProperties(
        String frontendOrigin,
        String oauth2SuccessRedirect,
        String oauth2FailureRedirect,
        Jwt jwt,
        Cookie cookie
) {
    public record Jwt(
            String issuer,
            String secret,
            long accessTokenMinutes,
            long refreshTokenDays
    ) {
    }

    public record Cookie(
            boolean secure,
            String sameSite
    ) {
    }
}

