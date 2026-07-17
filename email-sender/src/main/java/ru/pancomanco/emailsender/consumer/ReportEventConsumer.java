package ru.pancomanco.emailsender.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.pancomanco.emailsender.entity.ProcessedEvent;
import ru.pancomanco.emailsender.event.DailyReportEvent;
import ru.pancomanco.emailsender.exception.NonRetryableException;
import ru.pancomanco.emailsender.repository.ProcessedEventRepository;
import ru.pancomanco.emailsender.service.DailyReportEmailSender;

@Component
@Slf4j
public class ReportEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final DailyReportEmailSender dailyReportEmailSender;

    private final Counter reportsSent;
    private final Counter duplicatesSkipped;

    public ReportEventConsumer(ObjectMapper objectMapper,
                               ProcessedEventRepository processedEventRepository,
                               DailyReportEmailSender dailyReportEmailSender,
                               MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
        this.dailyReportEmailSender = dailyReportEmailSender;

        this.reportsSent = Counter.builder("emails.sent")
                .tag("type", "report")
                .description("Number of emails successfully sent")
                .register(meterRegistry);

        this.duplicatesSkipped = Counter.builder("emails.duplicates.skipped")
                .tag("type", "report")
                .description("Number of duplicate events skipped by idempotency")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "report-events", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleReportEvent(String payload, Acknowledgment acknowledgment) {
        DailyReportEvent event;
        try {
            event = objectMapper.readValue(payload, DailyReportEvent.class);
        } catch (JacksonException e) {
            throw new NonRetryableException("Malformed report event JSON", e);
        }

        if (processedEventRepository.existsById(event.eventId())) {
            log.debug("Report event {} already processed, skipping", event.eventId());
            duplicatesSkipped.increment();
            acknowledgment.acknowledge();
            return;
        }

        try {
            dailyReportEmailSender.sendReport(event);
            processedEventRepository.save(new ProcessedEvent(event.eventId(), "DailyReport"));
            reportsSent.increment();
            log.info("Processed DailyReport event {} for {}", event.eventId(), event.email());
        } catch (Exception e) {
            log.error("Failed to send report for event {}", event.eventId(), e);
            throw new NonRetryableException("Failed to send report", e);
        }
        acknowledgment.acknowledge();
    }
}
