package ru.pancomanco.authservice.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.pancomanco.authservice.config.EmailSender;
import ru.pancomanco.authservice.config.TestcontainersConfiguration;
import ru.pancomanco.authservice.dto.request.*;
import ru.pancomanco.authservice.entity.User;
import ru.pancomanco.authservice.repository.AuthRepository;
import ru.pancomanco.authservice.repository.EmailVerificationCodeRepository;
import ru.pancomanco.authservice.repository.PasswordResetTokenRepository;
import ru.pancomanco.authservice.repository.RefreshTokenRepository;
import ru.pancomanco.authservice.service.RateLimitService;
import ru.pancomanco.authservice.util.RefreshCookieHelper;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
@Import(TestcontainersConfiguration.class)
@Transactional
public class AuthFlowIT {

    private static final String PASSWORD = "Password123!";
    private static final String NEW_PASSWORD = "NewPassword456!";
    private static final String NAME = "FlowUser";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthRepository authRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private EmailVerificationCodeRepository codeRepository;
    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @MockitoBean
    private EmailSender emailSender;
    @MockitoBean
    private RateLimitService rateLimitService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.security.frontend-origin}")
    private String frontendOrigin;

    @BeforeEach
    void resetMocks() {
        reset(emailSender, rateLimitService);
    }

    private String uniqueEmail() {
        return "flow-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private String captureSentVerificationCode(String email) {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).sendVerificationCode(eq(email), codeCaptor.capture(),any(Locale.class));
        return codeCaptor.getValue();
    }

    private String captureSentResetLink(String email) {
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender, atLeastOnce()).sendPasswordResetLink(eq(email), linkCaptor.capture(),any(Locale.class));
        return linkCaptor.getValue();
    }

    private String extractTokenFromResetLink(String link) {
        int idx = link.indexOf("token=");
        return link.substring(idx + "token=".length());
    }

    private String extractRefreshTokenFromSetCookie(String setCookieHeader) {
        String prefix = RefreshCookieHelper.NAME + "=";
        int start = setCookieHeader.indexOf(prefix) + prefix.length();
        int end = setCookieHeader.indexOf(";", start);
        return setCookieHeader.substring(start, end == -1 ? setCookieHeader.length() : end);
    }

    private ResultActions performRegister(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .header(HttpHeaders.ORIGIN, frontendOrigin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequestDto(NAME, email, PASSWORD))));
    }

    private ResultActions performVerifyEmail(String email, String code) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/verify-email")
                .header(HttpHeaders.ORIGIN, frontendOrigin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new VerifyEmailRequestDto(email, code))));
    }

    private ResultActions performLogin(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, frontendOrigin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new LoginRequestDto(email, password))));
    }

    private ResultActions performRefresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie(RefreshCookieHelper.NAME, refreshToken))
                .header("Origin", frontendOrigin));
    }

    private ResultActions performLogout(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie(RefreshCookieHelper.NAME, refreshToken))
                .header("Origin", frontendOrigin));
    }

    private ResultActions performForgotPassword(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/forgot-password")
                .header(HttpHeaders.ORIGIN, frontendOrigin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ForgotPasswordRequestDto(email))));
    }

    private ResultActions performResetPassword(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/reset-password")
                .header(HttpHeaders.ORIGIN, frontendOrigin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ResetPasswordRequestDto(token, newPassword))));
    }

    @Test
    @DisplayName("Registration → verification → login")
    void fullRegistrationFlow_EndsWithWorkingSession() throws Exception {
        String email = uniqueEmail();

        performRegister(email).andExpect(status().isOk());

        User user = authRepository.findByEmail(email).orElseThrow();
        assertThat(user.getEnabled()).isFalse();

        String code = captureSentVerificationCode(email);

        performVerifyEmail(email, code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        User verified = authRepository.findByEmail(email).orElseThrow();
        assertThat(verified.getEnabled()).isTrue();

        reset(emailSender);

        performLogin(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    @DisplayName("Registration → verification → forgot → reset → login with new password")
    void passwordRecoveryFlow_EndsWithLoginUsingNewPassword() throws Exception {
        String email = uniqueEmail();

        performRegister(email).andExpect(status().isOk());
        String code = captureSentVerificationCode(email);
        performVerifyEmail(email, code).andExpect(status().isOk());

        reset(emailSender);

        performForgotPassword(email).andExpect(status().isOk());
        String resetLink = captureSentResetLink(email);
        String resetToken = extractTokenFromResetLink(resetLink);

        performResetPassword(resetToken, NEW_PASSWORD).andExpect(status().isOk());

        performLogin(email, PASSWORD).andExpect(status().isUnauthorized());

        performLogin(email, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("After refresh: new token works")
    void refreshFlow_NewTokenWorks() throws Exception {
        String email = uniqueEmail();
        performRegister(email).andExpect(status().isOk());
        String code = captureSentVerificationCode(email);
        performVerifyEmail(email, code).andExpect(status().isOk());
        reset(emailSender);

        String firstCookie = performLogin(email, PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String firstRefresh = extractRefreshTokenFromSetCookie(firstCookie);

        String secondCookie = performRefresh(firstRefresh)
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String secondRefresh = extractRefreshTokenFromSetCookie(secondCookie);

        assertThat(secondRefresh).isNotEqualTo(firstRefresh);

        performRefresh(secondRefresh).andExpect(status().isOk());
    }

    @Test
    @DisplayName("After refresh: old token triggers reuse detection")
    void refreshFlow_OldTokenTriggersReuseDetection() throws Exception {
        String email = uniqueEmail();
        performRegister(email).andExpect(status().isOk());
        String code = captureSentVerificationCode(email);
        performVerifyEmail(email, code).andExpect(status().isOk());
        reset(emailSender);

        String firstCookie = performLogin(email, PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String firstRefresh = extractRefreshTokenFromSetCookie(firstCookie);

        performRefresh(firstRefresh).andExpect(status().isOk());

        performRefresh(firstRefresh).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Login → logout → old cookie invalid")
    void logoutFlow_InvalidatesRefreshToken() throws Exception {
        String email = uniqueEmail();
        performRegister(email).andExpect(status().isOk());
        String code = captureSentVerificationCode(email);
        performVerifyEmail(email, code).andExpect(status().isOk());
        reset(emailSender);

        String setCookie = performLogin(email, PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String refreshToken = extractRefreshTokenFromSetCookie(setCookie);

        entityManager.flush();
        entityManager.clear();

        performLogout(refreshToken)
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        performRefresh(refreshToken).andExpect(status().isForbidden());
    }

    @Test
    void resetPassword_RevokesActiveSessionsAcrossFlow() throws Exception {
        String email = uniqueEmail();
        performRegister(email).andExpect(status().isOk());
        String code = captureSentVerificationCode(email);

        String verifyCookie = performVerifyEmail(email, code)
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String firstRefresh = extractRefreshTokenFromSetCookie(verifyCookie);

        String loginCookie = performLogin(email, PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String secondRefresh = extractRefreshTokenFromSetCookie(loginCookie);

        reset(emailSender);

        entityManager.flush();
        entityManager.clear();

        performForgotPassword(email).andExpect(status().isOk());
        String resetLink = captureSentResetLink(email);
        String resetToken = extractTokenFromResetLink(resetLink);

        performResetPassword(resetToken, NEW_PASSWORD).andExpect(status().isOk());

        performRefresh(firstRefresh).andExpect(status().isForbidden());
        performRefresh(secondRefresh).andExpect(status().isForbidden());



        User user = authRepository.findByEmail(email).orElseThrow();
        long active = refreshTokenRepository.findAll().stream()
                .filter(rt -> rt.getUser().getId().equals(user.getId()))
                .filter(rt -> !rt.isRevoked())
                .count();
        assertThat(active).isZero();
    }

    @Test
    void reregisterFlow_ReplacesPendingRegistration() throws Exception {
        String email = uniqueEmail();

        performRegister(email).andExpect(status().isOk());
        String firstCode = captureSentVerificationCode(email);

        reset(emailSender);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequestDto("UpdatedName", email, NEW_PASSWORD))))
                .andExpect(status().isOk());

        String secondCode = captureSentVerificationCode(email);
        assertThat(secondCode).isNotEqualTo(firstCode);

        performVerifyEmail(email, firstCode).andExpect(status().isBadRequest());

        performVerifyEmail(email, secondCode)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        User user = authRepository.findByEmail(email).orElseThrow();
        assertThat(user.getName()).isEqualTo("UpdatedName");
        assertThat(user.getEnabled()).isTrue();

        performLogin(email, PASSWORD).andExpect(status().isUnauthorized());
        performLogin(email, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Full lifecycle: register → verify → login → refresh → forgot → reset → login")
    void completeLifecycle_AllStepsConsistent() throws Exception {
        String email = uniqueEmail();

        performRegister(email).andExpect(status().isOk());
        String code = captureSentVerificationCode(email);

        performVerifyEmail(email, code).andExpect(status().isOk());
        reset(emailSender);

        String loginCookie = performLogin(email, PASSWORD)
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String firstRefresh = extractRefreshTokenFromSetCookie(loginCookie);

        String refreshCookie = performRefresh(firstRefresh)
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String secondRefresh = extractRefreshTokenFromSetCookie(refreshCookie);
        entityManager.flush();
        entityManager.clear();
        performForgotPassword(email).andExpect(status().isOk());
        String resetLink = captureSentResetLink(email);
        String resetToken = extractTokenFromResetLink(resetLink);

        performResetPassword(resetToken, NEW_PASSWORD).andExpect(status().isOk());

        performRefresh(secondRefresh).andExpect(status().isForbidden());

        performLogin(email, PASSWORD).andExpect(status().isUnauthorized());
        performLogin(email, NEW_PASSWORD).andExpect(status().isOk());
    }
}
