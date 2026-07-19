package ru.pancomanco.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequestDto(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        String email,

        @NotBlank(message = "{validation.verification_code.required}")
        @Pattern(
                regexp = "\\d{6}",
                message = "{validation.verification_code.format}"
        )
        String code
) {
}