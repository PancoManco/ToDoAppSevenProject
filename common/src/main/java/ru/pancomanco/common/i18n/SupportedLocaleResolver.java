package ru.pancomanco.common.i18n;

import java.util.Locale;
import java.util.Set;

public final class SupportedLocaleResolver {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private static final Set<String> SUPPORTED_LANGUAGES =
            Set.of("en", "ru");

    private SupportedLocaleResolver() {
    }

    public static Locale resolve(Locale requestedLocale) {
        if (requestedLocale == null) {
            return DEFAULT_LOCALE;
        }

        String language = requestedLocale.getLanguage();

        if (!SUPPORTED_LANGUAGES.contains(language)) {
            return DEFAULT_LOCALE;
        }

        return Locale.forLanguageTag(language);
    }
}
