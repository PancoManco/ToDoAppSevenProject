package ru.pancomanco.todoappsevenproject.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.pancomanco.todoappsevenproject.config.EmailSender;
import ru.pancomanco.todoappsevenproject.config.TestcontainersConfiguration;
import ru.pancomanco.todoappsevenproject.dto.request.*;
import ru.pancomanco.todoappsevenproject.entity.EmailVerificationCode;
import ru.pancomanco.todoappsevenproject.entity.PasswordResetToken;
import ru.pancomanco.todoappsevenproject.entity.RefreshToken;
import ru.pancomanco.todoappsevenproject.entity.User;
import ru.pancomanco.todoappsevenproject.repository.AuthRepository;
import ru.pancomanco.todoappsevenproject.repository.EmailVerificationCodeRepository;
import ru.pancomanco.todoappsevenproject.repository.PasswordResetTokenRepository;
import ru.pancomanco.todoappsevenproject.repository.RefreshTokenRepository;
import ru.pancomanco.todoappsevenproject.service.RateLimitService;
import ru.pancomanco.todoappsevenproject.service.TokenService;
import ru.pancomanco.todoappsevenproject.util.HashUtil;
import ru.pancomanco.todoappsevenproject.util.RefreshCookieHelper;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
public class AuthorizationControllerIT {
    private static final String VALID_PASSWORD = "Password123!";
    private static final String VALID_NAME = "TestUser";
    private static final String VALID_CODE = "123456";
    private static final String NEW_PASSWORD = "NewPassword456!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthRepository authRepository;
    @Autowired
    private EmailVerificationCodeRepository codeRepository;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private EmailSender emailSender;
    @MockitoBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void resetMocks() {
        reset(emailSender, rateLimitService);
    }

    @Value("${app.security.frontend-origin}")
    private String frontendOrigin;

    private String generateUniqueEmail() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private ResultActions performRegister(RegisterRequestDto request) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions performVerifyEmail(VerifyEmailRequestDto request) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions performLogin(LoginRequestDto request) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions performRefresh(String refreshTokenCookie, String origin) throws Exception {
        var request = post("/api/v1/auth/refresh");
        if (refreshTokenCookie != null) {
            request.cookie(new Cookie(RefreshCookieHelper.NAME, refreshTokenCookie));
        }
        if (origin != null) {
            request.header("Origin", origin);
        }
        return mockMvc.perform(request);
    }

    private ResultActions performLogout(String refreshTokenCookie, String origin) throws Exception {
        var request = post("/api/v1/auth/logout");
        if (refreshTokenCookie != null) {
            request.cookie(new Cookie(RefreshCookieHelper.NAME, refreshTokenCookie));
        }
        if (origin != null) {
            request.header("Origin", origin);
        }
        return mockMvc.perform(request);
    }

    private ResultActions performForgotPassword(ForgotPasswordRequestDto request) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions performResetPassword(ResetPasswordRequestDto request) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private void expectError(ResultActions result, int status, String messageKey) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(msg(messageKey)));
    }

    private String msg(String code) {
        return messageSource.getMessage(code, null, Locale.ENGLISH);
    }

    private User createUnverifiedUser(String email) {
        User user = new User(email, passwordEncoder.encode(VALID_PASSWORD));
        user.setName(VALID_NAME);
        user.setEnabled(false);
        return authRepository.save(user);
    }

    private User createVerifiedUser(String email) {
        User user = new User(email, passwordEncoder.encode(VALID_PASSWORD));
        user.setName(VALID_NAME);
        user.setEnabled(true);
        return authRepository.save(user);
    }

    private EmailVerificationCode createActiveCode(User user, String rawCode, Duration ttl) {
        EmailVerificationCode code = new EmailVerificationCode(
                user,
                passwordEncoder.encode(rawCode),
                Instant.now().plus(ttl)
        );
        return codeRepository.save(code);
    }

    private String loginAndExtractRefreshToken(String email) throws Exception {
        String setCookie = performLogin(new LoginRequestDto(email, VALID_PASSWORD))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        return extractRefreshTokenFromCookie(setCookie);
    }

    private String extractRefreshTokenFromCookie(String setCookieHeader) {
        String prefix = RefreshCookieHelper.NAME + "=";
        int start = setCookieHeader.indexOf(prefix) + prefix.length();
        int end = setCookieHeader.indexOf(";", start);
        return setCookieHeader.substring(start, end == -1 ? setCookieHeader.length() : end);
    }

    private String createActiveResetToken(User user, Duration ttl) {
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        String tokenHash = HashUtil.sha256Hex(rawToken);

        PasswordResetToken token = new PasswordResetToken(
                user,
                tokenHash,
                Instant.now().plus(ttl)
        );
        passwordResetTokenRepository.save(token);
        return rawToken;
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegistrationTests {

        @Test
        void register_NewUser_CreatesUnverifiedAndSendsCode() throws Exception {
            String email = generateUniqueEmail();

            performRegister(new RegisterRequestDto(VALID_NAME, email, VALID_PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.message").value(msg("auth.register.verification_sent")));

            assertThat(authRepository.findByEmail(email))
                    .isPresent()
                    .hasValueSatisfying(user -> {
                        assertThat(user.getEnabled()).isFalse();
                        assertThat(user.getName()).isEqualTo(VALID_NAME);
                        assertThat(user.getPassword()).isNotEqualTo(VALID_PASSWORD);
                        assertThat(passwordEncoder.matches(VALID_PASSWORD, user.getPassword())).isTrue();
                    });

            verify(emailSender, times(1)).sendVerificationCode(eq(email), anyString());
            verify(rateLimitService, times(1)).checkRegister(anyString(), eq(email));
        }

        @Test
        void register_ExistingUnverifiedUser_ResendsCodeAndUpdatesData() throws Exception {
            String email = generateUniqueEmail();
            User existing = new User(email, passwordEncoder.encode("oldPassword"));
            existing.setName("OldName");
            existing.setEnabled(false);
            authRepository.save(existing);

            performRegister(new RegisterRequestDto("NewName", email, "newPassword123"))
                    .andExpect(status().isOk());

            User updated = authRepository.findByEmail(email).orElseThrow();
            assertThat(updated.getName()).isEqualTo("NewName");
            assertThat(passwordEncoder.matches("newPassword123", updated.getPassword())).isTrue();
            assertThat(updated.getEnabled()).isFalse();

            verify(emailSender, times(1)).sendVerificationCode(eq(email), anyString());
        }

        @Test
        void register_VerifiedEmailExists_ReturnsConflict() throws Exception {
            String email = generateUniqueEmail();
            User verified = new User(email, passwordEncoder.encode(VALID_PASSWORD));
            verified.setName("Existing");
            verified.setEnabled(true);
            authRepository.save(verified);

            expectError(
                    performRegister(new RegisterRequestDto(VALID_NAME, email, VALID_PASSWORD)),
                    409,
                    "auth.email.already_exists"
            );

            verify(emailSender, never()).sendVerificationCode(anyString(), anyString());
        }

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    Arguments.of("name=null", new RegisterRequestDto(null, "valid@test.com", VALID_PASSWORD)),
                    Arguments.of("name=blank", new RegisterRequestDto("", "valid@test.com", VALID_PASSWORD)),
                    Arguments.of("name=whitespace", new RegisterRequestDto("   ", "valid@test.com", VALID_PASSWORD)),
                    Arguments.of("name=too short (1 char)", new RegisterRequestDto("a", "valid@test.com", VALID_PASSWORD)),
                    Arguments.of("name=too long (101 chars)", new RegisterRequestDto("a".repeat(101), "valid@test.com", VALID_PASSWORD)),

                    Arguments.of("email=null", new RegisterRequestDto(VALID_NAME, null, VALID_PASSWORD)),
                    Arguments.of("email=blank", new RegisterRequestDto(VALID_NAME, "", VALID_PASSWORD)),
                    Arguments.of("email=invalid format", new RegisterRequestDto(VALID_NAME, "not-an-email", VALID_PASSWORD)),
                    Arguments.of("email=whitespace", new RegisterRequestDto(VALID_NAME, "   ", VALID_PASSWORD)),
                    Arguments.of("email=missing @", new RegisterRequestDto(VALID_NAME, "test.com", VALID_PASSWORD)),

                    Arguments.of("password=null", new RegisterRequestDto(VALID_NAME, "valid@test.com", null)),
                    Arguments.of("password=blank", new RegisterRequestDto(VALID_NAME, "valid@test.com", "")),
                    Arguments.of("password=too short (5 chars)", new RegisterRequestDto(VALID_NAME, "valid@test.com", "12345")),
                    Arguments.of("password=too long (21 chars)", new RegisterRequestDto(VALID_NAME, "valid@test.com", "a".repeat(21)))
            );

        }

        @ParameterizedTest(name = "{0} → 400")
        @MethodSource("invalidRequests")
        void register_InvalidPayload_ReturnsBadRequest(String description, RegisterRequestDto request) throws Exception {
            performRegister(request).andExpect(status().isBadRequest());
            verify(emailSender, never()).sendVerificationCode(anyString(), anyString());
        }


        @Test
        void register_EmailWithMixedCaseAndSpaces_NormalizedInDatabase() throws Exception {
            String rawEmail = "MixedCase@Test.COM";
            String normalized = "mixedcase@test.com";

            performRegister(new RegisterRequestDto(VALID_NAME, rawEmail, VALID_PASSWORD))
                    .andExpect(status().isOk());

            assertThat(authRepository.findByEmail(normalized)).isPresent();
        }

        @Test
        void register_SameEmailDifferentCase_DetectsExistingVerifiedUser() throws Exception {
            String email = generateUniqueEmail();
            User verified = new User(email, passwordEncoder.encode(VALID_PASSWORD));
            verified.setEnabled(true);
            authRepository.save(verified);

            expectError(
                    performRegister(new RegisterRequestDto(VALID_NAME, email.toUpperCase(), VALID_PASSWORD)),
                    409,
                    "auth.email.already_exists"
            );
        }

        @Test
        void register_NewUser_CreatesVerificationCodeInDatabase() throws Exception {
            String email = generateUniqueEmail();

            performRegister(new RegisterRequestDto(VALID_NAME, email, VALID_PASSWORD))
                    .andExpect(status().isOk());

            User user = authRepository.findByEmail(email).orElseThrow();
            assertThat(codeRepository.findLatestActiveCodeForUpdate(user.getId()))
                    .isPresent()
                    .hasValueSatisfying(code -> {
                        assertThat(code.getCodeHash()).isNotBlank();
                        assertThat(code.isExpired()).isFalse();
                        assertThat(code.getAttempts()).isZero();
                    });
        }

        @Test
        void register_NewUser_SendsSixDigitCode() throws Exception {
            String email = generateUniqueEmail();

            performRegister(new RegisterRequestDto(VALID_NAME, email, VALID_PASSWORD))
                    .andExpect(status().isOk());

            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailSender).sendVerificationCode(eq(email), codeCaptor.capture());
            assertThat(codeCaptor.getValue()).matches("\\d{6}");
        }

    }

    @Nested
    @DisplayName("POST /api/v1/auth/verify-email")
    class EmailVerificationTests {

        @Test
        void verifyEmail_ValidCode_EnablesUserAndReturnsTokens() throws Exception {
            String email = generateUniqueEmail();
            User user = createUnverifiedUser(email);
            createActiveCode(user, VALID_CODE, Duration.ofMinutes(5));

            performVerifyEmail(new VerifyEmailRequestDto(email, VALID_CODE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().httpOnly("refresh_token", true))
                    .andExpect(cookie().path("refresh_token", "/api/v1/auth"));

            User updated = authRepository.findByEmail(email).orElseThrow();
            assertThat(updated.getEnabled()).isTrue();

            EmailVerificationCode code = codeRepository
                    .findLatestActiveCodeForUpdate(user.getId())
                    .orElse(null);
            assertThat(code).isNull();
        }

        @Test
        void verifyEmail_ValidCode_PersistsRefreshTokenInDatabase() throws Exception {
            String email = generateUniqueEmail();
            User user = createUnverifiedUser(email);
            createActiveCode(user, VALID_CODE, Duration.ofMinutes(5));

            performVerifyEmail(new VerifyEmailRequestDto(email, VALID_CODE))
                    .andExpect(status().isOk());

            assertThat(refreshTokenRepository.findAll())
                    .anyMatch(rt -> rt.getUser().getId().equals(user.getId())
                                    && !rt.isRevoked());
        }

        @Test
        void verifyEmail_WrongCode_IncrementsAttemptsAndFails() throws Exception {
            String email = generateUniqueEmail();
            User user = createUnverifiedUser(email);
            EmailVerificationCode code = createActiveCode(user, VALID_CODE, Duration.ofMinutes(5));

            expectError(
                    performVerifyEmail(new VerifyEmailRequestDto(email, "999999")),
                    400,
                    "auth.verification.code_invalid"
            );

            EmailVerificationCode reloaded = codeRepository.findById(code.getId()).orElseThrow();
            assertThat(reloaded.getAttempts()).isEqualTo(1);
            assertThat(reloaded.getUsedAt()).isNull();

            User reloadedUser = authRepository.findByEmail(email).orElseThrow();
            assertThat(reloadedUser.getEnabled()).isFalse();
        }

        @Test
        void verifyEmail_ExpiredCode_ReturnsErrorAndMarksUsed() throws Exception {
            String email = generateUniqueEmail();
            User user = createUnverifiedUser(email);
            EmailVerificationCode code = createActiveCode(user, VALID_CODE, Duration.ofMinutes(-1));

            expectError(
                    performVerifyEmail(new VerifyEmailRequestDto(email, VALID_CODE)),
                    400,
                    "auth.verification.code_expired"
            );

            EmailVerificationCode reloaded = codeRepository.findById(code.getId()).orElseThrow();
            assertThat(reloaded.getUsedAt()).isNotNull();
        }

        @Test
        void verifyEmail_TooManyAttempts_ReturnsTooManyRequests() throws Exception {
            String email = generateUniqueEmail();
            User user = createUnverifiedUser(email);
            EmailVerificationCode code = createActiveCode(user, VALID_CODE, Duration.ofMinutes(5));

            for (int i = 0; i < EmailVerificationCode.MAX_ATTEMPTS; i++) {
                code.increaseAttempts();
            }
            codeRepository.save(code);

            expectError(
                    performVerifyEmail(new VerifyEmailRequestDto(email, VALID_CODE)),
                    429,
                    "auth.verification.attempts_exceeded"
            );

            EmailVerificationCode reloaded = codeRepository.findById(code.getId()).orElseThrow();
            assertThat(reloaded.getUsedAt()).isNotNull();
        }

        @Test
        void verifyEmail_AlreadyVerified_ReturnsConflict() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);

            expectError(
                    performVerifyEmail(new VerifyEmailRequestDto(email, VALID_CODE)),
                    409,
                    "auth.email.already_verified"
            );
        }

        @Test
        void verifyEmail_NonExistentEmail_ReturnsGenericError() throws Exception {
            String email = generateUniqueEmail();
            expectError(
                    performVerifyEmail(new VerifyEmailRequestDto(email, VALID_CODE)),
                    400,
                    "auth.verification.code_invalid"
            );
        }

        @Test
        void verifyEmail_NoActiveCode_ReturnsError() throws Exception {
            String email = generateUniqueEmail();
            createUnverifiedUser(email);

            expectError(
                    performVerifyEmail(new VerifyEmailRequestDto(email, VALID_CODE)),
                    400,
                    "auth.verification.code_invalid"
            );
        }

        @Test
        void verifyEmail_WrongCode_AttemptsArePersistedDespiteException() throws Exception {
            String email = generateUniqueEmail();
            User user = createUnverifiedUser(email);
            EmailVerificationCode code = createActiveCode(user, VALID_CODE, Duration.ofMinutes(5));

            performVerifyEmail(new VerifyEmailRequestDto(email, "000000"))
                    .andExpect(status().isBadRequest());
            performVerifyEmail(new VerifyEmailRequestDto(email, "111111"))
                    .andExpect(status().isBadRequest());
            performVerifyEmail(new VerifyEmailRequestDto(email, "222222"))
                    .andExpect(status().isBadRequest());

            EmailVerificationCode reloaded = codeRepository.findById(code.getId()).orElseThrow();
            assertThat(reloaded.getAttempts()).isEqualTo(3);
        }
    }

    @Nested
    class Validation {

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    Arguments.of("email=null", new VerifyEmailRequestDto(null, VALID_CODE)),
                    Arguments.of("email=blank", new VerifyEmailRequestDto("", VALID_CODE)),
                    Arguments.of("email=invalid format", new VerifyEmailRequestDto("not-email", VALID_CODE)),
                    Arguments.of("code=null", new VerifyEmailRequestDto("valid@test.com", null)),
                    Arguments.of("code=blank", new VerifyEmailRequestDto("valid@test.com", "")),
                    Arguments.of("code=5 digits", new VerifyEmailRequestDto("valid@test.com", "12345")),
                    Arguments.of("code=7 digits", new VerifyEmailRequestDto("valid@test.com", "1234567")),
                    Arguments.of("code=letters", new VerifyEmailRequestDto("valid@test.com", "abcdef")),
                    Arguments.of("code=mixed", new VerifyEmailRequestDto("valid@test.com", "12345a"))
            );
        }

        @ParameterizedTest(name = "{0} → 400")
        @MethodSource("invalidRequests")
        void verifyEmail_InvalidPayload_ReturnsBadRequest(String description, VerifyEmailRequestDto request) throws Exception {
            performVerifyEmail(request).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        void login_ValidCredentials_ReturnsTokensAndCookie() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);

            performLogin(new LoginRequestDto(email, VALID_PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().httpOnly("refresh_token", true));

        }

        @Test
        void login_EmailWithDifferentCase_WorksCorrectly() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);

            performLogin(new LoginRequestDto(email.toUpperCase(), VALID_PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }

        @Test
        void login_Successful_PersistsRefreshTokenInDatabase() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);

            performLogin(new LoginRequestDto(email, VALID_PASSWORD))
                    .andExpect(status().isOk());

            assertThat(refreshTokenRepository.findAll())
                    .anyMatch(rt -> rt.getUser().getId().equals(user.getId()) && !rt.isRevoked());
        }

        @Test
        void login_NewSession_RevokesPreviousActiveTokens() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);

            performLogin(new LoginRequestDto(email, VALID_PASSWORD))
                    .andExpect(status().isOk());
            performLogin(new LoginRequestDto(email, VALID_PASSWORD))
                    .andExpect(status().isOk());

            entityManager.flush();
            entityManager.clear();

            long activeTokens = refreshTokenRepository.findAll().stream()
                    .filter(rt -> rt.getUser().getId().equals(user.getId()))
                    .filter(rt -> !rt.isRevoked())
                    .count();
            assertThat(activeTokens).isEqualTo(1);
        }

        @Test
        void login_Successful_CallsRateLimitCheck() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);

            performLogin(new LoginRequestDto(email, VALID_PASSWORD))
                    .andExpect(status().isOk());

            verify(rateLimitService, times(1)).checkLogin(anyString(), eq(email));
        }

        @Test
        void login_NonExistentEmail_ReturnsUnauthorized() throws Exception {
            expectError(
                    performLogin(new LoginRequestDto(generateUniqueEmail(), VALID_PASSWORD)),
                    401,
                    "auth.invalid_credentials"
            );
        }

        @Test
        void login_WrongPassword_ReturnsUnauthorized() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);

            expectError(
                    performLogin(new LoginRequestDto(email, "wrongPassword123")),
                    401,
                    "auth.invalid_credentials"
            );
        }

        @Test
        void login_UnverifiedUser_ReturnsForbidden() throws Exception {
            String email = generateUniqueEmail();
            createUnverifiedUser(email);

            expectError(
                    performLogin(new LoginRequestDto(email, VALID_PASSWORD)),
                    403,
                    "auth.email.not_verified"
            );
        }

        @Test
        void login_OAuthUserWithoutPassword_ReturnsUnauthorized() throws Exception {
            String email = generateUniqueEmail();
            User oauthUser = User.socialUser(email, "OAuth User", null);
            authRepository.save(oauthUser);

            expectError(
                    performLogin(new LoginRequestDto(email, VALID_PASSWORD)),
                    401,
                    "auth.invalid_credentials"
            );
        }

        @Test
        void login_NonExistentAndWrongPassword_ReturnSameErrorMessage() throws Exception {
            String existingEmail = generateUniqueEmail();
            createVerifiedUser(existingEmail);

            String nonExistentResponse = performLogin(
                    new LoginRequestDto(generateUniqueEmail(), VALID_PASSWORD))
                    .andReturn().getResponse().getContentAsString();

            String wrongPasswordResponse = performLogin(
                    new LoginRequestDto(existingEmail, "wrongPassword"))
                    .andReturn().getResponse().getContentAsString();

            assertThat(nonExistentResponse).isEqualTo(wrongPasswordResponse);
        }

        @Test
        void login_Failed_DoesNotCreateRefreshToken() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);

            performLogin(new LoginRequestDto(email, "wrongPassword"))
                    .andExpect(status().isUnauthorized());

            long tokens = refreshTokenRepository.findAll().stream()
                    .filter(rt -> rt.getUser().getId().equals(user.getId()))
                    .count();
            assertThat(tokens).isZero();
        }

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    Arguments.of("email=null", new LoginRequestDto(null, VALID_PASSWORD)),
                    Arguments.of("email=blank", new LoginRequestDto("", VALID_PASSWORD)),
                    Arguments.of("email=invalid format", new LoginRequestDto("not-email", VALID_PASSWORD)),
                    Arguments.of("email=whitespace", new LoginRequestDto("   ", VALID_PASSWORD)),
                    Arguments.of("password=null", new LoginRequestDto("valid@test.com", null)),
                    Arguments.of("password=blank", new LoginRequestDto("valid@test.com", ""))
            );
        }

        @ParameterizedTest(name = "{0} → 400")
        @MethodSource("invalidRequests")
        void login_InvalidPayload_ReturnsBadRequest(String description, LoginRequestDto request) throws Exception {
            performLogin(request).andExpect(status().isBadRequest());
        }

    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTests {
        @Test
        void refresh_ValidToken_ReturnsNewTokenPair() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);
            String refreshToken = loginAndExtractRefreshToken(email);

            performRefresh(refreshToken, frontendOrigin)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        }

        @Test
        void refresh_Successful_RevokesOldToken() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);
            String oldRefreshToken = loginAndExtractRefreshToken(email);
            String oldHash = HashUtil.sha256Hex(oldRefreshToken);

            performRefresh(oldRefreshToken, frontendOrigin)
                    .andExpect(status().isOk());

            RefreshToken oldToken = refreshTokenRepository
                    .findByTokenHashForUpdate(oldHash)
                    .orElseThrow();
            assertThat(oldToken.isRevoked()).isTrue();
        }

        @Test
        void refresh_Successful_PersistsNewToken() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String oldRefreshToken = loginAndExtractRefreshToken(email);

            String newSetCookie = performRefresh(oldRefreshToken, frontendOrigin)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);

            String newRefreshToken = extractRefreshTokenFromCookie(newSetCookie);
            assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

            String newHash = HashUtil.sha256Hex(newRefreshToken);
            RefreshToken newToken = refreshTokenRepository
                    .findByTokenHashForUpdate(newHash)
                    .orElseThrow();
            assertThat(newToken.isRevoked()).isFalse();
            assertThat(newToken.getUser().getId()).isEqualTo(user.getId());
        }

        @Test
        void refresh_ReusedToken_DetectsAndRevokesAllUserTokens() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String firstRefreshToken = loginAndExtractRefreshToken(email);

            performRefresh(firstRefreshToken, frontendOrigin)
                    .andExpect(status().isOk());

            expectError(
                    performRefresh(firstRefreshToken, frontendOrigin),
                    403,
                    "token.refresh_token_reuse_detected"
            );
            entityManager.flush();
            entityManager.clear();
            long activeTokens = refreshTokenRepository.findAll().stream()
                    .filter(rt -> rt.getUser().getId().equals(user.getId()))
                    .filter(rt -> !rt.isRevoked())
                    .count();
            assertThat(activeTokens).isZero();
        }


        @Test
        void refresh_MissingCookie_ReturnsUnauthorized() throws Exception {
            performRefresh(null, frontendOrigin)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Missing refresh token"));
        }

        @Test
        void refresh_MalformedJwt_ReturnsUnauthorized() throws Exception {
            expectError(
                    performRefresh("not-a-real-jwt", frontendOrigin),
                    401,
                    "token.invalid_refresh_token"
            );
        }

        @Test
        void refresh_UnknownTokenInDb_ReturnsUnauthorized() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String validJwt = tokenService.createRefreshToken(user);

            expectError(
                    performRefresh(validJwt, frontendOrigin),
                    401,
                    "token.invalid_refresh_token"
            );
        }

        @Test
        void refresh_AccessTokenInsteadOfRefresh_ReturnsUnauthorized() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String accessToken = tokenService.createAccessToken(user);

            expectError(
                    performRefresh(accessToken, frontendOrigin),
                    401,
                    "token.invalid_refresh_token"
            );
        }

        @Test
        void refresh_MissingOrigin_ReturnsForbidden() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);
            String refreshToken = loginAndExtractRefreshToken(email);

            performRefresh(refreshToken, null)
                    .andExpect(status().isForbidden());
        }

        @Test
        void refresh_ForeignOrigin_ReturnsForbidden() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);
            String refreshToken = loginAndExtractRefreshToken(email);

            performRefresh(refreshToken, "https://evil.com")
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        void logout_ValidToken_RevokesAndClearsCookie() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);
            String refreshToken = loginAndExtractRefreshToken(email);
            String tokenHash = HashUtil.sha256Hex(refreshToken);

            performLogout(refreshToken, frontendOrigin)
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().value("refresh_token", ""))
                    .andExpect(cookie().maxAge("refresh_token", 0))
                    .andExpect(cookie().httpOnly("refresh_token", true));

            entityManager.flush();
            entityManager.clear();
            RefreshToken stored = refreshTokenRepository
                    .findByTokenHashForUpdate(tokenHash)
                    .orElseThrow();
            assertThat(stored.isRevoked()).isTrue();
        }

        @Test
        void logout_MissingCookie_ReturnsNoContent() throws Exception {
            performLogout(null, frontendOrigin)
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().maxAge("refresh_token", 0));
        }

        @Test
        void logout_AlreadyRevokedToken_ReturnsNoContent() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);
            String refreshToken = loginAndExtractRefreshToken(email);

            performLogout(refreshToken, frontendOrigin)
                    .andExpect(status().isNoContent());

            performLogout(refreshToken, frontendOrigin)
                    .andExpect(status().isNoContent());
        }

        @Test
        void logout_UnknownToken_ReturnsNoContent() throws Exception {
            performLogout("not-a-real-token", frontendOrigin)
                    .andExpect(status().isNoContent());
        }

        @Test
        void logout_MissingOrigin_ReturnsForbidden() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);
            String refreshToken = loginAndExtractRefreshToken(email);

            performLogout(refreshToken, null)
                    .andExpect(status().isForbidden());
        }

        @Test
        void logout_ForeignOrigin_ReturnsForbidden() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);
            String refreshToken = loginAndExtractRefreshToken(email);

            performLogout(refreshToken, "https://evil.com")
                    .andExpect(status().isForbidden());
        }

        @Test
        void logout_ThenRefresh_TriggersReuseDetection() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);
            String refreshToken = loginAndExtractRefreshToken(email);

            performLogout(refreshToken, frontendOrigin)
                    .andExpect(status().isNoContent());
            entityManager.flush();
            entityManager.clear();
            expectError(
                    performRefresh(refreshToken, frontendOrigin),
                    403,
                    "token.refresh_token_reuse_detected"
            );
        }

        @Test
        void logout_DoesNotAffectOtherUsersTokens() throws Exception {
            String emailA = generateUniqueEmail();
            String emailB = generateUniqueEmail();
            User userA = createVerifiedUser(emailA);
            User userB = createVerifiedUser(emailB);

            String tokenA = loginAndExtractRefreshToken(emailA);
            String tokenB = loginAndExtractRefreshToken(emailB);

            performLogout(tokenA, frontendOrigin)
                    .andExpect(status().isNoContent());

            String hashB = HashUtil.sha256Hex(tokenB);
            RefreshToken storedB = refreshTokenRepository
                    .findByTokenHashForUpdate(hashB)
                    .orElseThrow();
            assertThat(storedB.isRevoked()).isFalse();

            performRefresh(tokenB, frontendOrigin)
                    .andExpect(status().isOk());
        }

    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPasswordTests {
        @Test
        void forgotPassword_VerifiedUser_CreatesTokenAndSendsEmail() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);

            performForgotPassword(new ForgotPasswordRequestDto(email))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(msg("auth.password.reset_link_sent")));

            assertThat(passwordResetTokenRepository.findAll())
                    .anyMatch(t -> t.getUser().getId().equals(user.getId())
                                   && t.getUsedAt() == null
                                   && !t.isExpired());

            verify(emailSender, times(1))
                    .sendPasswordResetLink(eq(email), anyString());
        }

        @Test
        void forgotPassword_SentLink_ContainsFrontendOriginAndToken() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);

            performForgotPassword(new ForgotPasswordRequestDto(email))
                    .andExpect(status().isOk());

            ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailSender).sendPasswordResetLink(eq(email), linkCaptor.capture());

            String link = linkCaptor.getValue();
            assertThat(link).startsWith(frontendOrigin);
            assertThat(link).contains("/reset-password?token=");
            assertThat(link).matches(".*token=[A-Za-z0-9_-]+$");
        }

        @Test
        void forgotPassword_RepeatedRequest_MarksPreviousTokensUsed() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);

            performForgotPassword(new ForgotPasswordRequestDto(email))
                    .andExpect(status().isOk());
            performForgotPassword(new ForgotPasswordRequestDto(email))
                    .andExpect(status().isOk());
            entityManager.flush();
            entityManager.clear();
            long activeTokens = passwordResetTokenRepository.findAll().stream()
                    .filter(t -> t.getUser().getId().equals(user.getId()))
                    .filter(t -> t.getUsedAt() == null)
                    .count();
            assertThat(activeTokens).isEqualTo(1);

            verify(emailSender, times(2))
                    .sendPasswordResetLink(eq(email), anyString());
        }

        @Test
        void forgotPassword_EmailWithDifferentCase_FindsUser() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);

            performForgotPassword(new ForgotPasswordRequestDto(email.toUpperCase()))
                    .andExpect(status().isOk());

            assertThat(passwordResetTokenRepository.findAll())
                    .anyMatch(t -> t.getUser().getId().equals(user.getId()));

            verify(emailSender, times(1)).sendPasswordResetLink(eq(email), anyString());
        }

        @Test
        void forgotPassword_Called_TriggersRateLimitCheck() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);

            performForgotPassword(new ForgotPasswordRequestDto(email))
                    .andExpect(status().isOk());

            verify(rateLimitService, times(1)).checkForgotPassword(anyString(), eq(email));
        }

        @Test
        void forgotPassword_NonExistentEmail_ReturnsOkAndDoesNothing() throws Exception {
            String email = generateUniqueEmail();

            performForgotPassword(new ForgotPasswordRequestDto(email))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(msg("auth.password.reset_link_sent")));

            assertThat(passwordResetTokenRepository.findAll()).isEmpty();
            verify(emailSender, never()).sendPasswordResetLink(anyString(), anyString());
        }

        @Test
        void forgotPassword_UnverifiedUser_ReturnsOkAndDoesNothing() throws Exception {
            String email = generateUniqueEmail();
            User user = createUnverifiedUser(email);

            performForgotPassword(new ForgotPasswordRequestDto(email))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(msg("auth.password.reset_link_sent")));

            long tokens = passwordResetTokenRepository.findAll().stream()
                    .filter(t -> t.getUser().getId().equals(user.getId()))
                    .count();
            assertThat(tokens).isZero();

            verify(emailSender, never()).sendPasswordResetLink(anyString(), anyString());
        }

        @Test
        void forgotPassword_ExistingAndNonExisting_ReturnIdenticalResponse() throws Exception {
            String existingEmail = generateUniqueEmail();
            createVerifiedUser(existingEmail);

            String existingResponse = performForgotPassword(new ForgotPasswordRequestDto(existingEmail))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String nonExistentResponse = performForgotPassword(new ForgotPasswordRequestDto(generateUniqueEmail()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(existingResponse).isEqualTo(nonExistentResponse);
        }

        @Test
        void forgotPassword_MailServerDown_ReturnsServiceUnavailable() throws Exception {
            String email = generateUniqueEmail();
            createVerifiedUser(email);

            doThrow(new org.springframework.mail.MailSendException("SMTP down"))
                    .when(emailSender).sendPasswordResetLink(eq(email), anyString());

            expectError(
                    performForgotPassword(new ForgotPasswordRequestDto(email)),
                    503,
                    "auth.password_reset.email_send_failed"
            );
        }

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    Arguments.of("email=null", new ForgotPasswordRequestDto(null)),
                    Arguments.of("email=blank", new ForgotPasswordRequestDto("")),
                    Arguments.of("email=invalid format", new ForgotPasswordRequestDto("not-email")),
                    Arguments.of("email=whitespace", new ForgotPasswordRequestDto("   ")),
                    Arguments.of("email=missing @", new ForgotPasswordRequestDto("test.com"))
            );
        }

        @ParameterizedTest(name = "{0} → 400")
        @MethodSource("invalidRequests")
        void forgotPassword_InvalidEmail_ReturnsBadRequest(String description, ForgotPasswordRequestDto request) throws Exception {
            performForgotPassword(request).andExpect(status().isBadRequest());

            verify(emailSender, never()).sendPasswordResetLink(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPasswordTests {
        @Test
        void resetPassword_ValidToken_ChangesPasswordAndMarksUsed() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String rawToken = createActiveResetToken(user, Duration.ofMinutes(15));

            performResetPassword(new ResetPasswordRequestDto(rawToken, NEW_PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(msg("auth.password.reset_success")));

            User reloaded = authRepository.findByEmail(email).orElseThrow();
            assertThat(passwordEncoder.matches(NEW_PASSWORD, reloaded.getPassword())).isTrue();
            assertThat(passwordEncoder.matches(VALID_PASSWORD, reloaded.getPassword())).isFalse();

            String tokenHash = HashUtil.sha256Hex(rawToken);
            PasswordResetToken usedToken = passwordResetTokenRepository
                    .findByTokenHashForUpdate(tokenHash)
                    .orElseThrow();
            assertThat(usedToken.getUsedAt()).isNotNull();
        }

        @Test
        void resetPassword_Successful_RevokesAllActiveRefreshTokens() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);

            loginAndExtractRefreshToken(email);
            String secondRefreshToken = loginAndExtractRefreshToken(email);
            String rawToken = createActiveResetToken(user, Duration.ofMinutes(15));

            performResetPassword(new ResetPasswordRequestDto(rawToken, NEW_PASSWORD))
                    .andExpect(status().isOk());
            entityManager.flush();
            entityManager.clear();
            long activeTokens = refreshTokenRepository.findAll().stream()
                    .filter(rt -> rt.getUser().getId().equals(user.getId()))
                    .filter(rt -> !rt.isRevoked())
                    .count();
            assertThat(activeTokens).isZero();
        }

        @Test
        void resetPassword_Successful_OldPasswordNoLongerWorks() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String rawToken = createActiveResetToken(user, Duration.ofMinutes(15));

            performResetPassword(new ResetPasswordRequestDto(rawToken, NEW_PASSWORD))
                    .andExpect(status().isOk());

            expectError(
                    performLogin(new LoginRequestDto(email, VALID_PASSWORD)),
                    401,
                    "auth.invalid_credentials"
            );
        }

        @Test
        void resetPassword_Successful_NewPasswordWorks() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String rawToken = createActiveResetToken(user, Duration.ofMinutes(15));

            performResetPassword(new ResetPasswordRequestDto(rawToken, NEW_PASSWORD))
                    .andExpect(status().isOk());

            performLogin(new LoginRequestDto(email, NEW_PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }

        @Test
        void resetPassword_Called_TriggersRateLimitCheck() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String rawToken = createActiveResetToken(user, Duration.ofMinutes(15));

            performResetPassword(new ResetPasswordRequestDto(rawToken, NEW_PASSWORD))
                    .andExpect(status().isOk());

            verify(rateLimitService, times(1)).checkResetPassword(anyString(), eq(rawToken));
        }

        @Test
        void resetPassword_UnknownToken_ReturnsBadRequest() throws Exception {
            expectError(
                    performResetPassword(new ResetPasswordRequestDto("non-existent-token", NEW_PASSWORD)),
                    400,
                    "auth.password_reset.token_invalid"
            );
        }

        @Test
        void resetPassword_AlreadyUsedToken_ReturnsBadRequest() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String rawToken = createActiveResetToken(user, Duration.ofMinutes(15));

            performResetPassword(new ResetPasswordRequestDto(rawToken, NEW_PASSWORD))
                    .andExpect(status().isOk());

            expectError(
                    performResetPassword(new ResetPasswordRequestDto(rawToken, "AnotherPassword789!")),
                    400,
                    "auth.password_reset.token_invalid"
            );
        }

        @Test
        void resetPassword_ExpiredToken_ReturnsBadRequestAndMarksUsed() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String rawToken = createActiveResetToken(user, Duration.ofMinutes(-1));

            expectError(
                    performResetPassword(new ResetPasswordRequestDto(rawToken, NEW_PASSWORD)),
                    400,
                    "auth.password_reset.token_expired"
            );

            String tokenHash = HashUtil.sha256Hex(rawToken);
            PasswordResetToken reloaded = passwordResetTokenRepository
                    .findByTokenHashForUpdate(tokenHash)
                    .orElseThrow();
            assertThat(reloaded.getUsedAt()).isNotNull();
        }

        @Test
        void resetPassword_InvalidToken_DoesNotChangePassword() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String originalHash = user.getPassword();

            performResetPassword(new ResetPasswordRequestDto("garbage-token", NEW_PASSWORD))
                    .andExpect(status().isBadRequest());

            User reloaded = authRepository.findByEmail(email).orElseThrow();
            assertThat(reloaded.getPassword()).isEqualTo(originalHash);
        }

        @Test
        void resetPassword_ExpiredToken_UsedAtPersistsDespiteException() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String rawToken = createActiveResetToken(user, Duration.ofMinutes(-1));

            performResetPassword(new ResetPasswordRequestDto(rawToken, NEW_PASSWORD))
                    .andExpect(status().isBadRequest());

            performResetPassword(new ResetPasswordRequestDto(rawToken, NEW_PASSWORD))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(msg("auth.password_reset.token_invalid")));
        }

        @Test
        void resetPassword_TokenBelongsToCorrectUser() throws Exception {
            String emailA = generateUniqueEmail();
            String emailB = generateUniqueEmail();
            User userA = createVerifiedUser(emailA);
            User userB = createVerifiedUser(emailB);
            String userAOriginalPassword = userA.getPassword();
            String userBOriginalPassword = userB.getPassword();

            String tokenForA = createActiveResetToken(userA, Duration.ofMinutes(15));

            performResetPassword(new ResetPasswordRequestDto(tokenForA, NEW_PASSWORD))
                    .andExpect(status().isOk());

            User reloadedA = authRepository.findByEmail(emailA).orElseThrow();
            User reloadedB = authRepository.findByEmail(emailB).orElseThrow();

            assertThat(reloadedA.getPassword()).isNotEqualTo(userAOriginalPassword);
            assertThat(reloadedB.getPassword()).isEqualTo(userBOriginalPassword);
        }

        @Test
        void resetPassword_ThenOldRefreshToken_TriggersReuseDetection() throws Exception {
            String email = generateUniqueEmail();
            User user = createVerifiedUser(email);
            String oldRefreshToken = loginAndExtractRefreshToken(email);
            String rawResetToken = createActiveResetToken(user, Duration.ofMinutes(15));

            performResetPassword(new ResetPasswordRequestDto(rawResetToken, NEW_PASSWORD))
                    .andExpect(status().isOk());
            entityManager.flush();
            entityManager.clear();
            expectError(
                    performRefresh(oldRefreshToken, frontendOrigin),
                    403,
                    "token.refresh_token_reuse_detected"
            );
        }

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    Arguments.of("token=null", new ResetPasswordRequestDto(null, NEW_PASSWORD)),
                    Arguments.of("token=blank", new ResetPasswordRequestDto("", NEW_PASSWORD)),
                    Arguments.of("password=null", new ResetPasswordRequestDto("some-token", null)),
                    Arguments.of("password=blank", new ResetPasswordRequestDto("some-token", "")),
                    Arguments.of("password=too short", new ResetPasswordRequestDto("some-token", "12345")),
                    Arguments.of("password=too long", new ResetPasswordRequestDto("some-token", "a".repeat(21)))
            );
        }

        @ParameterizedTest(name = "{0} → 400")
        @MethodSource("invalidRequests")
        @DisplayName("Невалидный запрос возвращает 400")
        void resetPassword_InvalidPayload_ReturnsBadRequest(String description, ResetPasswordRequestDto request) throws Exception {
            performResetPassword(request).andExpect(status().isBadRequest());
        }
    }


}


