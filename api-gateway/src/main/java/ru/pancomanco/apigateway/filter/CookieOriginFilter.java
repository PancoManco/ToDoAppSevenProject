package ru.pancomanco.apigateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.pancomanco.apigateway.properties.CookieOriginProperties;

@Component
@RequiredArgsConstructor
@Slf4j
public class CookieOriginFilter implements GlobalFilter, Ordered {
    private final CookieOriginProperties properties;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        if (!"POST".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        boolean isProtectedPath = properties.getProtectedPaths().stream()
                .anyMatch(path::equals);

        if (!isProtectedPath) {
            return chain.filter(exchange);
        }

        String origin = request.getHeaders().getFirst("Origin");

        if (origin == null || !origin.equals(properties.getAllowedOrigin())) {
            log.warn(
                    "SECURITY: Blocked request to {} with invalid/missing Origin: {}. Remote IP: {}",
                    path,
                    origin,
                    request.getRemoteAddress() != null
                            ? request.getRemoteAddress().getAddress().getHostAddress()
                            : "unknown"
            );

            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
