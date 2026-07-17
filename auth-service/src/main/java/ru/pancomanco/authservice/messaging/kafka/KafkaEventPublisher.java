package ru.pancomanco.authservice.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.pancomanco.authservice.messaging.exception.KafkaPublishException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {
    private static final int SEND_TIMEOUT_SECONDS = 10;
    private final KafkaTemplate<String, String> kafkaTemplate;

//    public void publish(String topic, String key, String payload) {
//        kafkaTemplate.send(topic, key, payload)
//                .whenComplete((result, ex) -> {
//                    if (ex != null) {
//                        log.error("Failed to publish to topic {}: {}", topic, ex.getMessage());
//                    } else {
//                        log.debug("Published to topic {} partition {} offset {}",
//                                topic,
//                                result.getRecordMetadata().partition(),
//                                result.getRecordMetadata().offset());
//                    }
//                });
//    }
    public void publish(String topic, String key, String payload) {
       try {
           kafkaTemplate.send(topic, key, payload).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
       }
       catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new KafkaPublishException(
                "Interrupted while publishing event " + key, e);

    } catch (TimeoutException e) {
        throw new KafkaPublishException(
                "Timeout publishing event " + key +
                        " after " + SEND_TIMEOUT_SECONDS + "s", e);
    } catch (Exception e) {
        throw new KafkaPublishException(
                "Failed to publish report event " + key, e);
    }
    }
}
