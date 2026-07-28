package ru.pancomanco.emailsender.event;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record UserVerifiedEvent(
        @NotBlank
        String eventId,

        @NotNull
        @Positive
        Long userId,

        @NotBlank
        @Size(max = 254)
        @Email
        String email,

        @Size(max = 100)
        String name,

        @NotNull
        Instant occurredAt
) {
}
