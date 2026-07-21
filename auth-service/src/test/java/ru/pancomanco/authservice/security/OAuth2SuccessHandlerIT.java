package ru.pancomanco.authservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.pancomanco.authservice.config.EmailSender;
import ru.pancomanco.authservice.config.TestcontainersConfiguration;
import ru.pancomanco.authservice.config.TestRateLimitConfig;
import ru.pancomanco.authservice.entity.User;
import ru.pancomanco.authservice.repository.AuthRepository;
import ru.pancomanco.authservice.repository.LinkedAccountRepository;
import ru.pancomanco.authservice.repository.RefreshTokenRepository;
import ru.pancomanco.authservice.service.RateLimitService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "app.oauth2.enabled=true",
        "spring.security.oauth2.client.registration.google.client-id=test-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
})
@Import({TestcontainersConfiguration.class, TestRateLimitConfig.class})
@ActiveProfiles("test")
class OAuth2SuccessHandlerIT {

    @Autowired private OAuth2SuccessHandler successHandler;
    @Autowired private AuthRepository authRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private LinkedAccountRepository linkedAccountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private EmailSender emailSender;
    @MockitoBean private RateLimitService rateLimitService;

    @Value("${app.security.oauth2-success-redirect}")
    private String successRedirect;

    @Value("${app.security.oauth2-failure-redirect}")
    private String failureRedirect;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        linkedAccountRepository.deleteAll();
        authRepository.deleteAll();
    }

    private String uniqueEmail() {
        return "oauth-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private OAuth2AuthenticationToken googleToken(String sub, String email, boolean emailVerified) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", sub);
        attributes.put("email", email);
        attributes.put("email_verified", emailVerified);
        attributes.put("name", "Google User");
        attributes.put("picture", "https://example.com/avatar.jpg");

        OAuth2User principal = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                attributes,
                "sub"
        );

        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    @Nested
    class SuccessfulAuth {

        @Test
        void onSuccess_NewUser_IssuesCookieAndRedirects() throws Exception {
            String email = uniqueEmail();
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);

            successHandler.onAuthenticationSuccess(
                    request, response, googleToken("sub-new", email, true));

            User created = authRepository.findByEmail(email).orElseThrow();
            assertThat(created.getEnabled()).isTrue();

            assertThat(refreshTokenRepository.findAll())
                    .anyMatch(rt -> rt.getUser().getId().equals(created.getId())
                                    && !rt.isRevoked());

            verify(response).addHeader(eq(HttpHeaders.SET_COOKIE),
                    contains("refresh_token="));
            verify(response).sendRedirect(successRedirect);
        }

        @Test
        void onSuccess_CookieHasSecurityAttributes() throws Exception {
            String email = uniqueEmail();
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);

            successHandler.onAuthenticationSuccess(
                    request, response, googleToken("sub-1", email, true));

            verify(response).addHeader(eq(HttpHeaders.SET_COOKIE),
                    argThat(cookie -> cookie.contains("HttpOnly")
                                      && cookie.contains("Path=/api/v1/auth")));
        }

        @Test
        void onSuccess_ExistingVerifiedUser_LinksAndRedirects() throws Exception {
            String email = uniqueEmail();
            User existing = new User(email, passwordEncoder.encode("Password123!"));
            existing.setName("testUser");
            existing.setEnabled(true);
            authRepository.save(existing);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);

            successHandler.onAuthenticationSuccess(
                    request, response, googleToken("sub-link", email, true));

            long userCount = authRepository.findAll().stream()
                    .filter(u -> u.getEmail().equals(email))
                    .count();
            assertThat(userCount).isEqualTo(1);

            verify(response).sendRedirect(successRedirect);
        }
    }

    @Nested
    class FailedAuth {

        @Test
        void onSuccess_UnverifiedConflict_RedirectsToFailure() throws Exception {
            String email = uniqueEmail();
            User unverified = new User(email, passwordEncoder.encode("Password123!"));
            unverified.setEnabled(false);
            authRepository.save(unverified);

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);

            successHandler.onAuthenticationSuccess(
                    request, response, googleToken("sub-conflict", email, true));

            verify(response).sendRedirect(failureRedirect);
            verify(response, never()).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());

            assertThat(refreshTokenRepository.findAll()).isEmpty();
        }

        @Test
        void onSuccess_InvalidProfile_RedirectsToFailure() throws Exception {
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);

            successHandler.onAuthenticationSuccess(
                    request, response, googleToken("sub-1", null, true));

            verify(response).sendRedirect(failureRedirect);
            verify(response, never()).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
        }

        @Test
        void onSuccess_UnverifiedGoogleEmail_RedirectsToFailure() throws Exception {
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);

            successHandler.onAuthenticationSuccess(
                    request, response, googleToken("sub-1", uniqueEmail(), false));

            verify(response).sendRedirect(failureRedirect);
        }
    }
}
