package ru.pancomanco.todoappsevenproject.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @Email
        @NotBlank
        String email,
        @NotBlank
        @Size(min = 6, max = 20)
        String password) {}

