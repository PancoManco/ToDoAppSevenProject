package ru.pancomanco.todoappsevenproject.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.todoappsevenproject.messaging.kafka.KafkaEventPublisher;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {

    private final OutboxRepository outboxRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long eventDbId) {
        OutboxEvent event = outboxRepository.findById(eventDbId)
                .orElseThrow();

        if (event.isPublished() || event.isDead()) {
            return;
        }

        try {
            kafkaEventPublisher.publish(
                    event.getTopic(),
                    event.getEventId(),
                    event.getPayload()
            );

            event.markPublished();

            log.debug("Published outbox event {}", event.getEventId());

        } catch (Exception e) {
            event.markFailed(e);

            log.warn(
                    "Outbox event {} failed. attempts={}, dead={}, nextAttemptAt={}",
                    event.getEventId(),
                    event.getAttempts(),
                    event.isDead(),
                    event.getNextAttemptAt()
            );

            throw e;
        }
    }
}