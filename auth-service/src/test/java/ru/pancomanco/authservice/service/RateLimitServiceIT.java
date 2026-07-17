package ru.pancomanco.authservice.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.pancomanco.authservice.config.TestcontainersConfiguration;
import ru.pancomanco.authservice.exception.RateLimitExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "app.rate-limit.enabled=true")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
public class RateLimitServiceIT {


    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(
                    DockerImageName.parse("redis:7-alpine")
            )
                    .withExposedPorts(6379);


    @DynamicPropertySource
    static void redisProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "spring.data.redis.host",
                redis::getHost
        );

        registry.add(
                "spring.data.redis.port",
                () -> redis.getMappedPort(6379)
        );
    }

    @Autowired
    private RateLimitService rateLimitService;

    private String uniqueIp() {
        return "192.168.1." + (int) (Math.random() * 255);
    }

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }


    @Nested
    class RegisterLimits {

        @Test
        void checkRegister_AllowsUnderIpLimit() {
            String ip = uniqueIp();

            for (int i = 0; i < 5; i++) {
                String email = uniqueEmail();
                assertThatCode(() -> rateLimitService.checkRegister(ip, email))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        void checkRegister_BlocksOverIpLimit() {
            String ip = uniqueIp();

            for (int i = 0; i < 5; i++) {
                rateLimitService.checkRegister(ip, uniqueEmail());
            }

            assertThatThrownBy(() -> rateLimitService.checkRegister(ip, uniqueEmail()))
                    .isInstanceOf(RateLimitExceededException.class);
        }

        @Test
        void checkRegister_BlocksOverEmailLimit() {
            String email = uniqueEmail();

            for (int i = 0; i < 3; i++) {
                rateLimitService.checkRegister(uniqueIp(), email);
            }

            assertThatThrownBy(() -> rateLimitService.checkRegister(uniqueIp(), email))
                    .isInstanceOf(RateLimitExceededException.class);
        }
    }

    @Nested
    class LoginLimits {

        @Test
        void checkLogin_BlocksOverIpLimit() {
            String ip = uniqueIp();

            for (int i = 0; i < 20; i++) {
                rateLimitService.checkLogin(ip, uniqueEmail());
            }

            assertThatThrownBy(() -> rateLimitService.checkLogin(ip, uniqueEmail()))
                    .isInstanceOf(RateLimitExceededException.class);
        }
        @Test
        void checkLogin_AllowsUnderEmailLimit() {
            String ip = uniqueIp();
            String email = uniqueEmail();

            for (int i = 0; i < 5; i++) {
                assertThatCode(() -> rateLimitService.checkLogin(ip, email))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        void checkLogin_BlocksOverEmailLimit() {
            String ip = uniqueIp();
            String email = uniqueEmail();

            for (int i = 0; i < 5; i++) {
                rateLimitService.checkLogin(ip, email);
            }

            assertThatThrownBy(() -> rateLimitService.checkLogin(ip, email))
                    .isInstanceOf(RateLimitExceededException.class)
                    .satisfies(ex -> {
                        RateLimitExceededException rateLimitEx = (RateLimitExceededException) ex;
                        assertThat(rateLimitEx.getRetryAfterSeconds()).isGreaterThan(0);
                    });
        }

        @Test
        void checkLogin_IsolatesLimitsByEmail() {
            String ip = uniqueIp();
            String emailA = uniqueEmail();
            String emailB = uniqueEmail();

            for (int i = 0; i < 5; i++) {
                rateLimitService.checkLogin(ip, emailA);
            }

            assertThatCode(() -> rateLimitService.checkLogin(uniqueIp(), emailB))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class VerifyEmailLimits {

        @Test
        void checkVerifyEmail_BlocksBruteForce() {
            String ip = uniqueIp();
            String email = uniqueEmail();

            for (int i = 0; i < 10; i++) {
                rateLimitService.checkVerifyEmail(ip, email);
            }

            assertThatThrownBy(() -> rateLimitService.checkVerifyEmail(ip, email))
                    .isInstanceOf(RateLimitExceededException.class);
        }

        @Test
        void checkVerifyEmail_DifferentIpDoesNotBypassEmailLimit() {
            String email = uniqueEmail();

            for (int i = 0; i < 10; i++) {
                rateLimitService.checkVerifyEmail(uniqueIp(), email);
            }

            assertThatThrownBy(() -> rateLimitService.checkVerifyEmail(uniqueIp(), email))
                    .isInstanceOf(RateLimitExceededException.class);
        }
    }

    @Nested
    class ForgotPasswordLimits {

        @Test
        void checkForgotPassword_BlocksOverEmailLimit() {
            String email = uniqueEmail();

            for (int i = 0; i < 3; i++) {
                rateLimitService.checkForgotPassword(uniqueIp(), email);
            }

            assertThatThrownBy(() -> rateLimitService.checkForgotPassword(uniqueIp(), email))
                    .isInstanceOf(RateLimitExceededException.class);
        }
    }

    @Nested
    class ResetPasswordLimits {

        @Test
        void checkResetPassword_BlocksOverTokenLimit() {
            String token = UUID.randomUUID().toString();

            for (int i = 0; i < 5; i++) {
                rateLimitService.checkResetPassword(uniqueIp(), token);
            }

            assertThatThrownBy(() -> rateLimitService.checkResetPassword(uniqueIp(), token))
                    .isInstanceOf(RateLimitExceededException.class);
        }

        @Test
        void checkResetPassword_IsolatesLimitsByToken() {
            String tokenA = UUID.randomUUID().toString();
            String tokenB = UUID.randomUUID().toString();

            for (int i = 0; i < 5; i++) {
                rateLimitService.checkResetPassword(uniqueIp(), tokenA);
            }

            assertThatCode(() -> rateLimitService.checkResetPassword(uniqueIp(), tokenB))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class RetryAfterContract {

        @Test
        void exceededLimit_ReturnsValidRetryAfter() {
            String ip = uniqueIp();
            String email = uniqueEmail();

            for (int i = 0; i < 5; i++) {
                rateLimitService.checkLogin(ip, email);
            }

            assertThatThrownBy(() -> rateLimitService.checkLogin(ip, email))
                    .isInstanceOfSatisfying(RateLimitExceededException.class, ex -> {
                        assertThat(ex.getRetryAfterSeconds())
                                .isGreaterThan(0)
                                .isLessThanOrEqualTo(60);
                    });
        }
    }
}
