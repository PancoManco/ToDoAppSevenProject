package ru.pancomanco.emailsender.event;

import java.time.Instant;
import java.util.List;

public record DailyReportEvent(
        String eventId,
        Long userId,
        String email,
        String name,
        int completedCount,
        List<String> completedTitles,
        int pendingCount,
        List<String> pendingTitles,
        Instant occurredAt
) {
}