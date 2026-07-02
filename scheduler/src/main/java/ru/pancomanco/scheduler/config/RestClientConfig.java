package ru.pancomanco.scheduler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient taskServiceRestClient(@Value("${app.task-service.url}") String taskServiceUrl) {
        return RestClient.builder()
                .baseUrl(taskServiceUrl)
                .build();
    }
}
