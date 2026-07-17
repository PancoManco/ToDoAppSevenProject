package ru.pancomanco.authservice.service;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import ru.pancomanco.authservice.config.TestRateLimitConfig;
import ru.pancomanco.authservice.messaging.outbox.OutboxEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import ru.pancomanco.authservice.config.TestcontainersConfiguration;
import ru.pancomanco.authservice.messaging.event.UserVerifiedEvent;

import ru.pancomanco.authservice.messaging.outbox.OutboxPoller;
import ru.pancomanco.authservice.messaging.outbox.OutboxRepository;
import ru.pancomanco.authservice.messaging.outbox.OutboxService;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestRateLimitConfig.class})
class OutboxIntegrationIT {

    @Autowired private OutboxService outboxService;
    @Autowired private OutboxPoller outboxPoller;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private ConsumerFactory<String, String> consumerFactory;
    @Autowired private ConfluentKafkaContainer kafkaContainer;
    @BeforeEach
    void clean() {
        outboxRepository.deleteAll();
    }

    @Test
    void save_WritesUnpublishedEvent() {
        String eventId = UUID.randomUUID().toString();
        UserVerifiedEvent event = new UserVerifiedEvent(
                eventId, 1L, "ivan@test.com", "Иван", Instant.now());

        outboxService.save(eventId, "UserVerified", "user-events", event);

        var all = outboxRepository.findAll();
        assertThat(all).hasSize(1);
        var saved = all.get(0);
        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getEventType()).isEqualTo("UserVerified");
        assertThat(saved.getTopic()).isEqualTo("user-events");
        assertThat(saved.isPublished()).isFalse();
        assertThat(saved.getPayload()).contains("ivan@test.com");
    }

    @Test
    void poller_PublishesAndMarksPublished() {
        String eventId = UUID.randomUUID().toString();
        UserVerifiedEvent event = new UserVerifiedEvent(
                eventId, 2L, "poll@test.com", "Пётр", Instant.now());
        outboxService.save(eventId, "UserVerified", "user-events", event);

        outboxPoller.pollAndPublish();

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var saved = outboxRepository.findById(
                            outboxRepository.findAll().get(0).getId()).orElseThrow();
                    assertThat(saved.isPublished()).isTrue();
                    assertThat(saved.getPublishedAt()).isNotNull();
                });

        try (Consumer<String, String> consumer = createTestConsumer()) {
            consumer.subscribe(java.util.List.of("user-events"));
            ConsumerRecords<String, String> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.value()).contains("poll@test.com");
            assertThat(record.key()).isEqualTo(eventId);
        }
    }

    @Test
    void outboxEvent_MarkFailed_IncrementsAttemptsAndSetsNextAttemptAt() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID().toString(),
                "UserVerified",
                "user-events",
                "{}"
        );

        event.markFailed(new RuntimeException("Kafka down"));

        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(event.getLastError()).contains("Kafka down");
        assertThat(event.isDead()).isFalse();
        assertThat(event.isPublished()).isFalse();
    }

    @Test
    void outboxEvent_MarkFailed_AfterMaxAttempts_MarksDead() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID().toString(),
                "UserVerified",
                "user-events",
                "{}"
        );

        for (int i = 0; i < 10; i++) {
            event.markFailed(new RuntimeException("Kafka down"));
        }

        assertThat(event.getAttempts()).isEqualTo(10);
        assertThat(event.isDead()).isTrue();
        assertThat(event.isPublished()).isFalse();
    }

    private Consumer<String, String> createTestConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }
}
