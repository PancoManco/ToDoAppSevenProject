package ru.pancomanco.todoappsevenproject.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.redis.data")
public record RateLimitProperties(String host, int port) {
}
