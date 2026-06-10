package ru.pancomanco.todoappsevenproject.service.impl;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.pancomanco.todoappsevenproject.exception.RateLimitExceededException;
import ru.pancomanco.todoappsevenproject.service.RateLimitService;
import ru.pancomanco.todoappsevenproject.util.HashUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl  implements RateLimitService {
    private final ProxyManager<byte[]> bucket4jProxyManager;

    @Override
    public void checkRegister(String ip, String email) {
        String emailHash = HashUtil.sha256Base64UrlNormalizedEmail(email);
        check("register:ip:" + ip, 5, Duration.ofMinutes(10));
        check("register:email:" + emailHash, 3, Duration.ofHours(1));
    }

    @Override
    public void checkLogin(String ip, String email) {
        String emailHash = HashUtil.sha256Base64UrlNormalizedEmail(email);
        check("login:ip:" + ip, 20, Duration.ofMinutes(1));
        check("login:email:" + emailHash, 5, Duration.ofMinutes(1));
        check("login:ip-email:" + ip + ":" + emailHash, 10, Duration.ofMinutes(1));
    }

    @Override
    public void checkVerifyEmail(String ip, String email) {
        String emailHash = HashUtil.sha256Base64UrlNormalizedEmail(email);
        check("verify-email:ip:" + ip, 30, Duration.ofMinutes(10));
        check("verify-email:email:" + emailHash, 10, Duration.ofMinutes(10));
    }

    @Override
    public void checkResendVerification(String ip, String email) {
        String emailHash = HashUtil.sha256Base64UrlNormalizedEmail(email);
        check("resend-verification:ip:" + ip, 5, Duration.ofMinutes(10));
        check("resend-verification:email:" + emailHash, 2, Duration.ofMinutes(1));
        check("resend-verification:email-hour:" + emailHash, 5, Duration.ofHours(1));
    }

    @Override
    public void checkForgotPassword(String ip, String email) {
        String emailHash = HashUtil.sha256Base64UrlNormalizedEmail(email);
        check("forgot-password:ip:" + ip, 10, Duration.ofMinutes(15));
        check("forgot-password:email:" + emailHash, 3, Duration.ofMinutes(15));
        check("forgot-password:email-hour:" + emailHash, 5, Duration.ofHours(1));
    }

    @Override
    public void checkResetPassword(String ip, String token) {
        check("reset-password:ip:" + ip, 10, Duration.ofMinutes(10));
        check("reset-password:token:" + HashUtil.sha256Base64Url(token), 5, Duration.ofMinutes(10));
    }

    private void check(String key, long capacity, Duration refillPeriod) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillPeriod)
                        .build())
                .build();

        Bucket bucket = bucket4jProxyManager
                .builder()
                .build(key.getBytes(StandardCharsets.UTF_8), configuration);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(
                    1,
                    TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())
            );

            throw new RateLimitExceededException(retryAfterSeconds);
        }
    }



}
