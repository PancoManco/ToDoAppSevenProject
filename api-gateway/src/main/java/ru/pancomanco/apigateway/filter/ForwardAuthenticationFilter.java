package ru.pancomanco.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;


@Component
@Slf4j
public class ForwardAuthenticationFilter
        extends AbstractGatewayFilterFactory<ForwardAuthenticationFilter.Config> {

    public ForwardAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    if (!(securityContext.getAuthentication() instanceof JwtAuthenticationToken jwtAuth)) {
                        return Mono.just(exchange);
                    }

                    var jwt = jwtAuth.getToken();


                    String userId = jwt.getSubject();
                    String email = jwt.getClaimAsString("email");
                    List<String> roles = jwt.getClaimAsStringList("roles");
                    String tokenType = jwt.getClaimAsString("token_type");

                    log.debug("Forwarding authenticated request: userId={}, email={}, path={}",
                            userId, email, exchange.getRequest().getURI().getPath());

                    var mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Email", email != null ? email : "")
                            .header("X-User-Roles", roles != null ? String.join(",", roles) : "")
                            .header("X-Token-Type", tokenType != null ? tokenType : "")
                            .build();

                    return Mono.just(exchange.mutate().request(mutatedRequest).build());
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    public static class Config {
    }
}