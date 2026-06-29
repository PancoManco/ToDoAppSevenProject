package ru.pancomanco.taskservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequestDto(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String description
) {
}
