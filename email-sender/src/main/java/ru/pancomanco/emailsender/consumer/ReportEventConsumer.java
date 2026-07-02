package ru.pancomanco.emailsender.consumer;


import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
public class ReportEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final DailyReportEmailSender dailyReportEmailSender;

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
            acknowledgment.acknowledge();
            return;
        }

        processedEventRepository.save(new ProcessedEvent(event.eventId(), "DailyReport"));
        dailyReportEmailSender.sendReport(event);
        acknowledgment.acknowledge();

        log.info("Processed DailyReport event {} for {}", event.eventId(), event.email());
    }
}
