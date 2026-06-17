package ru.pancomanco.todoappsevenproject.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "false")
public class TestRateLimitConfig {
    @Bean
    @SuppressWarnings("unchecked")
    ProxyManager<byte[]> bucket4jProxyManager() {
        return Mockito.mock(ProxyManager.class);
    }
}
