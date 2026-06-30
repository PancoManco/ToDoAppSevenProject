package ru.pancomanco.todoappsevenproject.messaging.event;

import java.time.Instant;

public record UserVerifiedEvent(
        String eventId,
        Long userId,
        String email,
        String name,
        Instant occurredAt
) {
}
