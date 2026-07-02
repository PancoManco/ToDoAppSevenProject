package ru.pancomanco.emailsender.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import ru.pancomanco.emailsender.exception.NonRetryableException;

@Configuration
@Slf4j
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3));

        handler.addNotRetryableExceptions(NonRetryableException.class);

        handler.setRetryListeners((record, ex, attempt) ->
                log.warn("Retry {} for record from {}: {}", attempt, record.topic(), ex.getMessage()));

        return handler;
    }
}
