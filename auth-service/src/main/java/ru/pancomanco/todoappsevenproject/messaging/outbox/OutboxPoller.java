package ru.pancomanco.todoappsevenproject.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.messaging.kafka.KafkaEventPublisher;



import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private static final int BATCH_SIZE = 100;

    private final OutboxRepository outboxRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> events = outboxRepository
                .findByPublishedFalseOrderByCreatedAtAsc(Limit.of(BATCH_SIZE));

        if (events.isEmpty()) {
            return;
        }

        log.debug("Found {} unpublished outbox events", events.size());

        for (OutboxEvent event : events) {
            kafkaEventPublisher.publish(
                    event.getTopic(),
                    event.getEventId(),
                    event.getPayload()
            );
            event.markPublished();
        }
    }
}
