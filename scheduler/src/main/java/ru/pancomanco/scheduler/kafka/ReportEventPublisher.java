package ru.pancomanco.scheduler.kafka;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.pancomanco.scheduler.event.DailyReportEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.report.topic}")
    private String reportTopic;

    public void publish(DailyReportEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(reportTopic, event.eventId(), payload).get();
            log.debug("Published report event {} for user {}", event.eventId(), event.userId());
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to publish report event " + event.eventId(), e);
        }
    }
}
