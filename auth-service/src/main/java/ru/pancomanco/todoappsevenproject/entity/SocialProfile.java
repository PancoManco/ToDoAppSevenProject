package ru.pancomanco.todoappsevenproject.entity;

public record SocialProfile(
        String providerUserId,
        String email,
        String name,
        String avatarUrl
) {
}
