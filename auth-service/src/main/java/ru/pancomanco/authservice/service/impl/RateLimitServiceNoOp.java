package ru.pancomanco.authservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.pancomanco.authservice.service.RateLimitService;

@Service
@Slf4j
@ConditionalOnProperty(
        name = "app.rate-limit.enabled",
        havingValue = "false"
)
public class RateLimitServiceNoOp implements RateLimitService {

    public RateLimitServiceNoOp() {
        log.warn("⚠️ Rate limiting is DISABLED — all requests will pass through");
    }

    @Override
    public void checkRegister(String ip, String email) {

    }

    @Override
    public void checkLogin(String ip, String email) {

    }

    @Override
    public void checkVerifyEmail(String ip, String email) {

    }

    @Override
    public void checkResendVerification(String ip, String email) {

    }

    @Override
    public void checkForgotPassword(String ip, String email) {

    }

    @Override
    public void checkResetPassword(String ip, String token) {

    }
}
