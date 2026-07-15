package ru.pancomanco.emailsender.consumer;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import ru.pancomanco.emailsender.config.TestMailConfig;
import ru.pancomanco.emailsender.config.TestcontainersConfiguration;
import ru.pancomanco.emailsender.event.UserVerifiedEvent;
import ru.pancomanco.emailsender.repository.ProcessedEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestMailConfig.class})
class UserEventConsumerIT {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    @Autowired
    private JavaMailSender javaMailSender;

    @BeforeEach
    void clean() {
        processedEventRepository.deleteAll();
        reset(javaMailSender);
    }

    private UserVerifiedEvent newEvent(String eventId, String email) {
        return new UserVerifiedEvent(eventId, 1L, email, "Иван", Instant.now());
    }

    private void publish(UserVerifiedEvent event) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("user-events", event.eventId(), payload).get();
    }

    @Test
    void consumesEvent_SendsEmailAndRecordsProcessed() throws Exception {
        String eventId = UUID.randomUUID().toString();
        publish(newEvent(eventId, "ivan@test.com"));

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
                    assertThat(processedEventRepository.existsById(eventId)).isTrue();
                });
    }

    @Test
    void duplicateEvent_DoesNotSendSecondEmail() throws Exception {
        String eventId = UUID.randomUUID().toString();
        UserVerifiedEvent event = newEvent(eventId, "dup@test.com");

        publish(event);
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertThat(processedEventRepository.existsById(eventId)).isTrue());

        publish(event);
        Awaitility.await()
                .during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class))
                );

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

}
