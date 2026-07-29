package ru.pancomanco.authservice.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.oauth2.google")
@ConditionalOnProperty(name = "app.oauth2.enabled", havingValue = "true")
public record OAuth2ClientProperties(
        @NotBlank(message = "Google Client ID is required when OAuth2 is enabled")
        String clientId,

        @NotBlank(message = "Google Client Secret is required when OAuth2 is enabled")
        String clientSecret
) {
}
