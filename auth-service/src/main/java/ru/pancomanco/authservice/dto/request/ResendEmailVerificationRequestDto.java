package ru.pancomanco.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendEmailVerificationRequestDto(
        @Email
        @NotBlank
        String email
) {
}