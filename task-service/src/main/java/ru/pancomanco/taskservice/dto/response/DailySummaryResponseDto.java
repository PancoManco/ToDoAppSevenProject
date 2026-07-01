package ru.pancomanco.taskservice.dto.response;

import java.util.List;

public record DailySummaryResponseDto(
        List<UserTaskSummary> users
) {
    public record UserTaskSummary(
            Long userId,
            String email,
            String name,
            int completedCount,
            List<String> completedTitles,
            int pendingCount,
            List<String> pendingTitles
    ) {
    }
}
