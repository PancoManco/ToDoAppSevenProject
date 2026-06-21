package ru.pancomanco.todoappsevenproject.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDto(
        @Email
        @NotBlank
        String email
) {
}
