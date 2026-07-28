package ru.pancomanco.apigateway.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        boolean enabled,

        @NotEmpty
        List<@NotBlank @URL String> allowedOrigins
) {
}
