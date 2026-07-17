package ru.pancomanco.authservice.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private static final int BATCH_SIZE = 100;
    private final OutboxRepository outboxRepository;
    private final OutboxEventProcessor outboxEventProcessor;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    public void pollAndPublish() {
        List<Long> eventIds = outboxRepository.findReadyToPublishIds(
                Instant.now(),
                Limit.of(BATCH_SIZE)
        );

        if (eventIds.isEmpty()) {
            return;
        }

        int published = 0;
        int failed = 0;

        for (Long eventId : eventIds) {
            try {
                outboxEventProcessor.process(eventId);
                published++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to publish outbox event id={}, will retry later", eventId, e);
            }
        }
        log.info("Outbox poll finished: {} published, {} failed", published, failed);
        }
    }
