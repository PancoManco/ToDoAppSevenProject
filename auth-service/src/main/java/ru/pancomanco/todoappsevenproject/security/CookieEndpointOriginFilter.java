package ru.pancomanco.todoappsevenproject.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.pancomanco.todoappsevenproject.properties.AuthProperties;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class CookieEndpointOriginFilter extends OncePerRequestFilter {

    private final String allowedOrigin;

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    );

    public CookieEndpointOriginFilter(AuthProperties properties) {
        this.allowedOrigin = properties.frontendOrigin();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {

        boolean isProtectedCookieEndpoint =
                "POST".equalsIgnoreCase(request.getMethod())
                && PROTECTED_PATHS.contains(request.getRequestURI());

        if (isProtectedCookieEndpoint) {
            String origin = request.getHeader("Origin");

            if (origin == null || !origin.equals(allowedOrigin)) {
                log.warn("SECURITY: Blocked request to {} with invalid/missing Origin: {}. Remote IP: {}",
                        request.getRequestURI(), origin, request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Origin");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}