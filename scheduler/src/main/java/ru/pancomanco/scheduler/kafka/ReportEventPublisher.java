package ru.pancomanco.scheduler.kafka;

import ru.pancomanco.scheduler.exception.ReportPublishException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.pancomanco.scheduler.event.DailyReportEvent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportEventPublisher {

    private static final int SEND_TIMEOUT_SECONDS = 10;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.report.topic}")
    private String reportTopic;

    public void publish(DailyReportEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(reportTopic, event.eventId(), payload).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.debug("Published report event {} for user {}", event.eventId(), event.userId());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReportPublishException(
                    "Interrupted while publishing event " + event.eventId(), e);

        } catch (TimeoutException e) {
            throw new ReportPublishException(
                    "Timeout publishing event " + event.eventId() +
                            " after " + SEND_TIMEOUT_SECONDS + "s", e);
        } catch (Exception e) {
            throw new ReportPublishException(
                    "Failed to publish report event " + event.eventId(), e);
        }
    }
}
