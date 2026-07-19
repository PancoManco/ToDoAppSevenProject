package ru.pancomanco.common.i18n;


import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CommonI18nAutoConfiguration {

    @Bean
    @ConditionalOnBean(MessageSource.class)
    @ConditionalOnMissingBean(MessageService.class)
    public MessageService messageService(MessageSource messageSource) {
        return new MessageService(messageSource);
    }
}