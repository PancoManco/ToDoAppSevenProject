package ru.pancomanco.todoappsevenproject.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.data.redis")
public record RateLimitProperties(String host, int port) {
}
