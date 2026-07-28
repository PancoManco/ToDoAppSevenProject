package ru.pancomanco.taskservice.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.internal")
public record InternalProperties(
        @NotBlank String apiKey
) {
}
