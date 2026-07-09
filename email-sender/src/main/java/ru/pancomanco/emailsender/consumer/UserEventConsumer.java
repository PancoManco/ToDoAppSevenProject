package ru.pancomanco.emailsender.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import liquibase.license.User;
import org.springframework.mail.MailException;
import ru.pancomanco.emailsender.exception.NonRetryableException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.emailsender.entity.ProcessedEvent;
import ru.pancomanco.emailsender.event.UserVerifiedEvent;
import ru.pancomanco.emailsender.repository.ProcessedEventRepository;
import ru.pancomanco.emailsender.service.WelcomeEmailSender;

@Component
@Slf4j
public class UserEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final WelcomeEmailSender welcomeEmailSender;

    private final Counter emailsSent;
    private final Counter duplicatesSkipped;

    public UserEventConsumer(ObjectMapper objectMapper,
                             ProcessedEventRepository processedEventRepository,
                             WelcomeEmailSender welcomeEmailSender,
                             MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
        this.welcomeEmailSender = welcomeEmailSender;

        this.emailsSent = Counter.builder("emails.sent")
                .tag("type", "welcome")
                .description("Number of emails successfully sent")
                .register(meterRegistry);

        this.duplicatesSkipped = Counter.builder("emails.duplicates.skipped")
                .tag("type", "welcome")
                .description("Number of duplicate events skipped by idempotency")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "user-events", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleUserEvent(String payload, Acknowledgment acknowledgment,MeterRegistry meterRegistry) {
        UserVerifiedEvent event;
        try {
            event = objectMapper.readValue(payload, UserVerifiedEvent.class);
        } catch (JacksonException e) {
            throw new NonRetryableException("Malformed event JSON", e);
        }

        if (processedEventRepository.existsById(event.eventId())) {
            log.debug("Event {} already processed, skipping", event.eventId());
            duplicatesSkipped.increment();
            acknowledgment.acknowledge();
            return;
        }

        processedEventRepository.save(new ProcessedEvent(event.eventId(), "UserVerified"));
        welcomeEmailSender.sendWelcomeEmail(event.email(), event.name());
        emailsSent.increment();
        acknowledgment.acknowledge();

        log.info("Processed UserVerified event {} for {}", event.eventId(), event.email());
    }
}
