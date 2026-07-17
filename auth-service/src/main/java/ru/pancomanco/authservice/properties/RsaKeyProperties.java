package ru.pancomanco.authservice.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.rsa")
public record RsaKeyProperties(
        String privateKeyBase64,
        String publicKeyBase64
) {
}
