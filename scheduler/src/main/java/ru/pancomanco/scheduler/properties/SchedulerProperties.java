package ru.pancomanco.scheduler.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record SchedulerProperties(
        @NotNull
        @Valid
        TaskService taskService,

        @NotNull
        @Valid
        Internal internal,

        @NotNull
        @Valid
        Report report
) {

    public record TaskService(
            @NotBlank
            @URL
            String url
    ) {
    }

    public record Internal(
            @NotBlank
            @Size(min = 32, max = 256)
            String apiKey
    ) {
    }

    public record Report(
            @NotBlank
            @Pattern(
                    regexp = "^[a-zA-Z0-9._-]+$",
                    message = "Invalid Kafka topic"
            )
            String topic,

            @NotBlank
            String cron
    ) {
        public Report {
            if (cron != null
                && !cron.isBlank()
                && !CronExpression.isValidExpression(cron)) {
                throw new IllegalArgumentException(
                        "Invalid report cron: " + cron
                );
            }
        }
    }
}
