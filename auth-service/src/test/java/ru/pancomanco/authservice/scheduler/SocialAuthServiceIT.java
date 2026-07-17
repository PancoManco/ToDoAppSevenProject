package ru.pancomanco.authservice.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.pancomanco.authservice.config.EmailSender;
import ru.pancomanco.authservice.config.TestRateLimitConfig;
import ru.pancomanco.authservice.config.TestcontainersConfiguration;
import ru.pancomanco.authservice.entity.AuthProviderEnum;
import ru.pancomanco.authservice.entity.User;
import ru.pancomanco.authservice.exception.ErrorCode;
import ru.pancomanco.authservice.exception.SocialAuthException;
import ru.pancomanco.authservice.repository.AuthRepository;
import ru.pancomanco.authservice.repository.LinkedAccountRepository;
import ru.pancomanco.authservice.service.RateLimitService;
import ru.pancomanco.authservice.service.SocialAuthService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, TestRateLimitConfig.class})
@ActiveProfiles("test")
public class SocialAuthServiceIT {

    @Autowired private SocialAuthService socialAuthService;
    @Autowired private AuthRepository authRepository;
    @Autowired private LinkedAccountRepository linkedAccountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private EmailSender emailSender;
    @MockitoBean private RateLimitService rateLimitService;

    @BeforeEach
    void cleanDatabase() {
        linkedAccountRepository.deleteAll();
        authRepository.deleteAll();
    }

    private String uniqueEmail() {
        return "oauth-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private Map<String, Object> googleAttributes(String sub, String email, boolean emailVerified) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("sub", sub);
        attrs.put("email", email);
        attrs.put("email_verified", emailVerified);
        attrs.put("name", "Google User");
        attrs.put("picture", "https://example.com/avatar.jpg");
        return attrs;
    }
    @Nested
    class NewUserCreation {

        @Test
        void findOrCreateUser_NewGoogleUser_CreatesUserAndLink() {
            String email = uniqueEmail();
            String sub = "google-sub-123";

            User result = socialAuthService.findOrCreateUser(
                    "google", googleAttributes(sub, email, true));

            assertThat(result.getId()).isNotNull();
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.getEnabled()).isTrue();
            assertThat(result.getPassword()).isNull();

            User stored = authRepository.findByEmail(email).orElseThrow();
            assertThat(stored.getEnabled()).isTrue();

            assertThat(linkedAccountRepository
                    .findByProviderAndProviderUserIdWithUser(AuthProviderEnum.GOOGLE, sub))
                    .isPresent()
                    .hasValueSatisfying(la -> {
                        assertThat(la.getUser().getId()).isEqualTo(result.getId());
                        assertThat(la.getProviderEmail()).isEqualTo(email);
                    });
        }

        @Test
        void findOrCreateUser_MixedCaseEmail_NormalizesEmail() {
            String email = uniqueEmail();
            String upperEmail = email.toUpperCase();

            User result = socialAuthService.findOrCreateUser(
                    "google", googleAttributes("sub-1", upperEmail, true));

            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(authRepository.findByEmail(email)).isPresent();
        }

        @Test
        void findOrCreateUser_MissingName_UsesEmailAsName() {
            String email = uniqueEmail();
            Map<String, Object> attrs = googleAttributes("sub-1", email, true);
            attrs.remove("name");

            User result = socialAuthService.findOrCreateUser("google", attrs);

            assertThat(result.getName()).isEqualTo(email);
        }
    }

    @Nested
    class ExistingUserLogin {

        @Test
        void findOrCreateUser_ExistingLinkedAccount_ReturnsSameUser() {
            String email = uniqueEmail();
            String sub = "google-sub-456";

            User first = socialAuthService.findOrCreateUser(
                    "google", googleAttributes(sub, email, true));
            User second = socialAuthService.findOrCreateUser(
                    "google", googleAttributes(sub, email, true));

            assertThat(second.getId()).isEqualTo(first.getId());

            long linkCount = linkedAccountRepository.findAll().stream()
                    .filter(la -> la.getUser().getId().equals(first.getId()))
                    .count();
            assertThat(linkCount).isEqualTo(1);
        }

        @Test
        void findOrCreateUser_MultipleLogins_NoDuplicateUsers() {
            String email = uniqueEmail();
            String sub = "google-sub-789";

            socialAuthService.findOrCreateUser("google", googleAttributes(sub, email, true));
            socialAuthService.findOrCreateUser("google", googleAttributes(sub, email, true));
            socialAuthService.findOrCreateUser("google", googleAttributes(sub, email, true));

            long userCount = authRepository.findAll().stream()
                    .filter(u -> u.getEmail().equals(email))
                    .count();
            assertThat(userCount).isEqualTo(1);
        }
    }

    @Nested
    class AccountLinking {

        @Test
        void findOrCreateUser_ExistingVerifiedUser_LinksGoogleAccount() {
            String email = uniqueEmail();
            User existing = new User(email, passwordEncoder.encode("Password123!"));
            existing.setName("LocalUser");
            existing.setEnabled(true);
            authRepository.save(existing);

            User result = socialAuthService.findOrCreateUser(
                    "google", googleAttributes("sub-link", email, true));

            assertThat(result.getId()).isEqualTo(existing.getId());
            assertThat(result.getPassword()).isNotNull();

            assertThat(linkedAccountRepository
                    .existsByUserAndProvider(existing, AuthProviderEnum.GOOGLE))
                    .isTrue();
        }

        @Test
        void findOrCreateUser_AlreadyLinkedUser_DoesNotDuplicateLink() {
            String email = uniqueEmail();
            User existing = new User(email, passwordEncoder.encode("Password123!"));
            existing.setEnabled(true);
            authRepository.save(existing);

            socialAuthService.findOrCreateUser("google", googleAttributes("sub-x", email, true));
            socialAuthService.findOrCreateUser("google", googleAttributes("sub-x", email, true));

            long linkCount = linkedAccountRepository.findAll().stream()
                    .filter(la -> la.getUser().getId().equals(existing.getId()))
                    .count();
            assertThat(linkCount).isEqualTo(1);
        }
    }

    @Nested
    class Conflicts {

        @Test
        void findOrCreateUser_UnverifiedLocalUser_ThrowsConflict() {
            String email = uniqueEmail();
            User unverified = new User(email, passwordEncoder.encode("Password123!"));
            unverified.setEnabled(false);
            authRepository.save(unverified);

            assertThatThrownBy(() -> socialAuthService.findOrCreateUser(
                    "google", googleAttributes("sub-conflict", email, true)))
                    .isInstanceOf(SocialAuthException.class)
                    .satisfies(ex -> assertThat(((SocialAuthException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_SOCIAL_UNVERIFIED_EMAIL_CONFLICT));

            assertThat(linkedAccountRepository.findAll()).isEmpty();
        }
    }

    @Nested
    class ProfileValidation {

        @Test
        void findOrCreateUser_MissingSub_ThrowsProfileInvalid() {
            Map<String, Object> attrs = googleAttributes(null, uniqueEmail(), true);

            assertThatThrownBy(() -> socialAuthService.findOrCreateUser("google", attrs))
                    .isInstanceOf(SocialAuthException.class)
                    .satisfies(ex -> assertThat(((SocialAuthException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_SOCIAL_PROFILE_INVALID));
        }

        @Test
        void findOrCreateUser_MissingEmail_ThrowsEmailMissing() {
            Map<String, Object> attrs = googleAttributes("sub-1", null, true);

            assertThatThrownBy(() -> socialAuthService.findOrCreateUser("google", attrs))
                    .isInstanceOf(SocialAuthException.class)
                    .satisfies(ex -> assertThat(((SocialAuthException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_SOCIAL_EMAIL_MISSING));
        }

        @Test
        void findOrCreateUser_UnverifiedGoogleEmail_ThrowsEmailNotVerified() {
            Map<String, Object> attrs = googleAttributes("sub-1", uniqueEmail(), false);

            assertThatThrownBy(() -> socialAuthService.findOrCreateUser("google", attrs))
                    .isInstanceOf(SocialAuthException.class)
                    .satisfies(ex -> assertThat(((SocialAuthException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_SOCIAL_EMAIL_NOT_VERIFIED));
        }

        @Test
        void findOrCreateUser_BlankSub_ThrowsProfileInvalid() {
            Map<String, Object> attrs = googleAttributes("   ", uniqueEmail(), true);

            assertThatThrownBy(() -> socialAuthService.findOrCreateUser("google", attrs))
                    .isInstanceOf(SocialAuthException.class)
                    .satisfies(ex -> assertThat(((SocialAuthException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_SOCIAL_PROFILE_INVALID));
        }
    }

    @Nested
    class ProviderValidation {

        @Test
        void findOrCreateUser_NullProvider_ThrowsUnsupported() {
            Map<String, Object> attrs = googleAttributes("sub-1", uniqueEmail(), true);

            assertThatThrownBy(() -> socialAuthService.findOrCreateUser(null, attrs))
                    .isInstanceOf(SocialAuthException.class)
                    .satisfies(ex -> assertThat(((SocialAuthException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED));
        }

        @Test
        void findOrCreateUser_UnsupportedProvider_ThrowsUnsupported() {
            Map<String, Object> attrs = googleAttributes("sub-1", uniqueEmail(), true);

            assertThatThrownBy(() -> socialAuthService.findOrCreateUser("facebook", attrs))
                    .isInstanceOf(SocialAuthException.class)
                    .satisfies(ex -> assertThat(((SocialAuthException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED));
        }

        @Test
        void findOrCreateUser_UppercaseProvider_Accepted() {
            String email = uniqueEmail();

            User result = socialAuthService.findOrCreateUser(
                    "GOOGLE", googleAttributes("sub-upper", email, true));

            assertThat(result.getEmail()).isEqualTo(email);
        }
    }
}