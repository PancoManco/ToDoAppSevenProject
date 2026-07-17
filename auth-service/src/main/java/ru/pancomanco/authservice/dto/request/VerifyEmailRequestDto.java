package ru.pancomanco.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequestDto(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "Code must contain 6 digits")
        String code
) {
}