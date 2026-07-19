package ru.pancomanco.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(

        @NotBlank(message = "{validation.token.required}")
        String token,

        @NotBlank(message = "{validation.password.required}")
        @Size(
                min = 8,
                max = 64,
                message = "{validation.password.size}"
        )
        String newPassword
) {
}
