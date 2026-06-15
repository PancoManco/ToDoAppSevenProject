package ru.pancomanco.todoappsevenproject.service.impl;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.pancomanco.todoappsevenproject.config.RateLimitConfig;
import ru.pancomanco.todoappsevenproject.exception.ErrorCode;
import ru.pancomanco.todoappsevenproject.exception.RateLimitExceededException;
import ru.pancomanco.todoappsevenproject.service.RateLimitService;
import ru.pancomanco.todoappsevenproject.util.HashUtil;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl  implements RateLimitService {
    private final ProxyManager<byte[]> bucket4jProxyManager;

    @Override
    public void checkRegister(String ip, String email) {
        String emailHash = HashUtil.sha256Base64UrlNormalizedEmail(email);
        check("register:ip:" + ip, RateLimitConfig.REGISTER_IP_LIMIT);
        check("register:email:" + emailHash, RateLimitConfig.REGISTER_EMAIL_LIMIT);
    }

    @Override
    public void checkLogin(String ip, String email) {
        String emailHash = HashUtil.sha256Base64UrlNormalizedEmail(email);
        check("login:ip:" + ip, RateLimitConfig.LOGIN_IP_LIMIT);
        check("login:email:" + emailHash, RateLimitConfig.LOGIN_EMAIL_LIMIT);
        check("login:ip-email:" + ip + ":" + emailHash, RateLimitConfig.LOGIN_IP_EMAIL_LIMIT);
    }

    @Override
    public void checkVerifyEmail(String ip, String email) {
        String emailHash = HashUtil.sha256Base64UrlNormalizedEmail(email);
        check("verify-email:ip:" + ip, RateLimitConfig.VERIFY_EMAIL_IP_LIMIT);
        check("verify-email:email:" + emailHash, RateLimitConfig.VERIFY_EMAIL_EMAIL_LIMIT);
    }

    @Override
    public void checkResendVerification(String ip, String email) {
        String emailHash = HashUtil.sha256Base64UrlNormalizedEmail(email);
        check("resend-verification:ip:" + ip, RateLimitConfig.RESEND_VERIFICATION_IP_LIMIT);
        check("resend-verification:email:" + emailHash, RateLimitConfig.RESEND_VERIFICATION_EMAIL_LIMIT);
        check("resend-verification:email-hour:" + emailHash, RateLimitConfig.RESEND_VERIFICATION_EMAIL_HOUR_LIMIT);
    }

    @Override
    public void checkForgotPassword(String ip, String email) {
        String emailHash = HashUtil.sha256Base64UrlNormalizedEmail(email);
        check("forgot-password:ip:" + ip, RateLimitConfig.FORGOT_PASSWORD_IP_LIMIT);
        check("forgot-password:email:" + emailHash, RateLimitConfig.FORGOT_PASSWORD_EMAIL_LIMIT);
        check("forgot-password:email-hour:" + emailHash, RateLimitConfig.FORGOT_PASSWORD_EMAIL_HOUR_LIMIT);
    }

    @Override
    public void checkResetPassword(String ip, String token) {
        check("reset-password:ip:" + ip, RateLimitConfig.RESET_PASSWORD_IP_LIMIT);
        check("reset-password:token:" + HashUtil.sha256Base64Url(token), RateLimitConfig.RESET_PASSWORD_TOKEN_LIMIT);
    }

    private void check(String key, BucketConfiguration configuration) {
        Bucket bucket = bucket4jProxyManager
                .builder()
                .build(key.getBytes(StandardCharsets.UTF_8), configuration);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(
                    1,
                    TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())
            );
            throw new RateLimitExceededException(ErrorCode.AUTH_RATE_LIMIT_EXCEEDED, retryAfterSeconds);
        }
    }

}
