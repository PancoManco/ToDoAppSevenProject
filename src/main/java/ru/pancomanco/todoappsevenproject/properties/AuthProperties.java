package ru.pancomanco.todoappsevenproject.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "app.security")
public record AuthProperties(String frontendOrigin, Jwt jwt) {
    public record Jwt(
            String issuer,
            String secret,
            long accessTokenMinutes,
            long refreshTokenDays
    ) {
    }
}

