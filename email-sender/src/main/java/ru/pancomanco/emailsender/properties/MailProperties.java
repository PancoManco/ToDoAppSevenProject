package ru.pancomanco.emailsender.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        String from,
        String defaultLocale
) {

    public Locale locale() {
        return Locale.forLanguageTag(defaultLocale);
    }
}
