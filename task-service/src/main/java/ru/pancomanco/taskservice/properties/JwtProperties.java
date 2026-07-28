package ru.pancomanco.taskservice.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank
        @Size(max = 200)
        String issuer,

        @NotBlank
        @URL
        String jwkSetUri
) {
}
