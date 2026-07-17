package ru.pancomanco.authservice.messaging.outbox;


import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void save(String eventId, String eventType, String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(eventId, eventType, topic, payload);
            outboxRepository.save(outboxEvent);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize event for outbox", e);
        }
    }
}
