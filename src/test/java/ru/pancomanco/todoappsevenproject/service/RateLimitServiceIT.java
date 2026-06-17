package ru.pancomanco.todoappsevenproject.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.pancomanco.todoappsevenproject.config.TestcontainersConfiguration;
import ru.pancomanco.todoappsevenproject.exception.RateLimitExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class RateLimitServiceIT {

    @Autowired
    private RateLimitService rateLimitService;

    private String uniqueIp() {
        return "192.168.1." + (int) (Math.random() * 255);
    }

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    @Test
    void checkLogin_ShouldAllowRequestsUnderLimit() {
        String ip = uniqueIp();
        String email = uniqueEmail();

        for (int i = 0; i < 4; i++) {
            assertThatCode(() -> rateLimitService.checkLogin(ip, email))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void checkLogin_ShouldBlockRequestsOverLimit() {
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
    void checkLogin_ShouldIsolateLimitsByUser() {
        String ip = uniqueIp();
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();

        for (int i = 0; i < 5; i++) {
            rateLimitService.checkLogin(ip, emailA);
        }


        assertThatCode(() -> rateLimitService.checkLogin(ip, emailB))
                .doesNotThrowAnyException();
    }

    @Test
    void checkLogin_ShouldEnforceCombinedIpEmailLimit() {
        String ip = uniqueIp();
        String email = uniqueEmail();

        for (int i = 0; i < 5; i++) {
            rateLimitService.checkLogin(ip, email);
        }

        assertThatThrownBy(() -> rateLimitService.checkLogin(ip, email))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void checkVerifyEmail_ShouldBlockBruteForceAttempts() {
        String ip = uniqueIp();
        String email = uniqueEmail();

        for (int i = 0; i < 10; i++) {
            assertThatCode(() -> rateLimitService.checkVerifyEmail(ip, email))
                    .doesNotThrowAnyException();
        }

        assertThatThrownBy(() -> rateLimitService.checkVerifyEmail(ip, email))
                .isInstanceOf(RateLimitExceededException.class);

        String differentIp = uniqueIp();
        assertThatCode(() -> rateLimitService.checkVerifyEmail(differentIp, email))
                .doesNotThrowAnyException();
    }

}
