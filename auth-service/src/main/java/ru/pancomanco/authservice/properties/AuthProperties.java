package ru.pancomanco.authservice.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Validated
@ConfigurationProperties(prefix = "app.security")
public record AuthProperties(
        @NotBlank
        @URL
        String frontendOrigin,

        @NotBlank
        @URL
        String oauth2SuccessRedirect,

        @NotBlank
        @URL
        String oauth2FailureRedirect,

        @NotNull
        @Valid
        Jwt jwt,

        @NotNull
        @Valid
        Cookie cookie
) {

    public record Jwt(
            @NotBlank
            @Size(max = 200)
            String issuer,

            @Positive
            long accessTokenMinutes,

            @Positive
            long refreshTokenDays
    ) {
    }

    public record Cookie(
            boolean secure,

            @NotBlank
            @Pattern(
                    regexp = "(?i)Strict|Lax|None",
                    message = "sameSite must be Strict, Lax or None"
            )
            String sameSite
    ) {
    }
}

