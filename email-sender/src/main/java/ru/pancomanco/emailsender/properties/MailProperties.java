package ru.pancomanco.emailsender.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Locale;

@Validated
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        @NotBlank
        @Email
        String from,

        @NotBlank
        @Pattern(
                regexp = "ru|en",
                message = "defaultLocale must be ru or en"
        )
        String defaultLocale
) {
    public Locale locale() {
        return Locale.forLanguageTag(defaultLocale);
    }
}
