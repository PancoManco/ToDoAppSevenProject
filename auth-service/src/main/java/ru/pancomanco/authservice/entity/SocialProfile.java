package ru.pancomanco.authservice.entity;

public record SocialProfile(
        String providerUserId,
        String email,
        String name,
        String avatarUrl
) {
}
