package ru.pancomanco.authservice.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.rsa")
public record RsaKeyProperties(
        @NotBlank String privateKeyBase64,
        @NotBlank String publicKeyBase64
) {
}
