package ru.pancomanco.taskservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequestDto(

        @NotBlank(
                message = "{validation.task.title.required}"
        )
        @Size(
                max = 255,
                message = "{validation.task.title.size}"
        )
        String title,

        @Size(
                max = 5000,
                message = "{validation.task.description.size}"
        )
        String description

) {
}
