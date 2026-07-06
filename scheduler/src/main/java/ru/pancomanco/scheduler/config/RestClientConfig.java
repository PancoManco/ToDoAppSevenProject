package ru.pancomanco.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.pancomanco.scheduler.properties.SchedulerProperties;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient taskServiceRestClient(SchedulerProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.taskService().url())
                .defaultHeader("X-Internal-Api-Key", properties.internal().apiKey())
                .build();
    }
}
