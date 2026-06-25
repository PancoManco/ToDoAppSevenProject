package ru.pancomanco.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
            String forwardedFor = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                ip = forwardedFor.split(",")[0].trim();
            }
            return Mono.just("gateway:ip:" + ip);
        };
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .filter(principal -> principal instanceof JwtAuthenticationToken)
                .map(principal -> {
                    var jwt = ((JwtAuthenticationToken) principal)
                            .getToken();
                    return "gateway:user:" + jwt.getSubject();
                })
                .defaultIfEmpty("gateway:anonymous");
    }

    }

