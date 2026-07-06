package ru.pancomanco.scheduler.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
public record SchedulerProperties(
        TaskService taskService,
        Internal internal,
        Report report
) {
    public record TaskService(String url) {
    }

    public record Internal(String apiKey) {
    }

    public record Report(String topic, String cron) {
    }
}
