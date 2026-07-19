package ru.pancomanco.common.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    public String get(String code, Object... args) {
        return messageSource.getMessage(
                code,
                args,
                code,
                LocaleContextHolder.getLocale()
        );
    }
    public String get(
            Locale locale,
            String code,
            Object... args
    ) {
        return messageSource.getMessage(
                code,
                args,
                locale
        );
    }

    public String getOrDefault(
            Locale locale,
            String code,
            String defaultMessage,
            Object... args
    ) {
        return messageSource.getMessage(
                code,
                args,
                defaultMessage,
                locale
        );
    }
}