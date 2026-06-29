package ru.pancomanco.taskservice.dto.response;

import ru.pancomanco.taskservice.entity.Task;

import java.time.Instant;

public record TaskResponseDto(
        Long id,
        String title,
        String description,
        boolean completed,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponseDto from(Task task) {
        return new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
