package ru.pancomanco.todoappsevenproject.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxCleanupJob {

    private static final int RETENTION_DAYS = 7;

    private final OutboxRepository outboxRepository;

    @Scheduled(cron = "${app.outbox.cleanup-cron:0 0 2 * * *}")
    @Transactional
    public void cleanupPublishedEvents() {
        Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        int deleted = outboxRepository.deletePublishedOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Outbox cleanup: deleted {} published events older than {} days",
                    deleted, RETENTION_DAYS);
        }
    }
}
