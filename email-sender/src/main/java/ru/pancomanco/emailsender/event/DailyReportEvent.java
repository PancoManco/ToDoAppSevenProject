package ru.pancomanco.emailsender.event;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public record DailyReportEvent(
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

        @PositiveOrZero
        int completedCount,

        @NotNull
        @Size(max = 5)
        List<
                @NotBlank
                @Size(max = 255)
                        String
                > completedTitles,

        @PositiveOrZero
        int pendingCount,

        @NotNull
        @Size(max = 5)
        List<
                @NotBlank
                @Size(max = 255)
                        String
                > pendingTitles,

        @NotNull
        Instant occurredAt
) {
}