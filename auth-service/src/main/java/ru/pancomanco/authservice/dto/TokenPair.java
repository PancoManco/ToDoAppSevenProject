package ru.pancomanco.authservice.dto;

public record TokenPair(String accessToken,
                        String refreshToken) {
}
