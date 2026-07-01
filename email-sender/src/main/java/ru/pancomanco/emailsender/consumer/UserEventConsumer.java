package ru.pancomanco.emailsender.consumer;

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
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final WelcomeEmailSender welcomeEmailSender;

    @KafkaListener(topics = "user-events", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleUserEvent(String payload, Acknowledgment acknowledgment) {
        try {
            UserVerifiedEvent event = objectMapper.readValue(payload, UserVerifiedEvent.class);

            if (processedEventRepository.existsById(event.eventId())) {
                log.debug("Event {} already processed, skipping", event.eventId());
                acknowledgment.acknowledge();
                return;
            }

            processedEventRepository.save(new ProcessedEvent(event.eventId(), "UserVerified"));

            welcomeEmailSender.sendWelcomeEmail(event.email(), event.name());

            acknowledgment.acknowledge();

            log.info("Processed UserVerified event {} for {}", event.eventId(), event.email());

        } catch (Exception e) {
            log.error("Failed to process event: {}", payload, e);
            throw new RuntimeException("Event processing failed", e);
        }
    }
}
