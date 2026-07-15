package ru.pancomanco.taskservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.pancomanco.taskservice.properties.InternalProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Slf4j
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_PREFIX = "/internal/";

    private final String expectedApiKey;

    public InternalApiKeyFilter(InternalProperties properties) {
        this.expectedApiKey = properties.apiKey();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith(INTERNAL_PREFIX)) {
            String providedKey = request.getHeader(HEADER);

            if (providedKey == null || !constantTimeEquals(providedKey, expectedApiKey)) {
                log.warn("Rejected internal request to {} — invalid/missing API key. IP: {}",
                        request.getRequestURI(), request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"forbidden\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String provided, String expected) {
        if (provided == null || provided.isBlank() || expected == null || expected.isBlank()) {
            return false;
        }

        byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(providedBytes, expectedBytes);
    }
}
