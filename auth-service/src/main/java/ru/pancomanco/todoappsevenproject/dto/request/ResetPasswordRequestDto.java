package ru.pancomanco.todoappsevenproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(
        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 64)
        String newPassword
) {
}
