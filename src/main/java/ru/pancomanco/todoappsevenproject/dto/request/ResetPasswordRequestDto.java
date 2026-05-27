package ru.pancomanco.todoappsevenproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(
        @NotBlank
        String token,

        @NotBlank
        @Size(min = 6, max = 20)
        String newPassword
) {
}
