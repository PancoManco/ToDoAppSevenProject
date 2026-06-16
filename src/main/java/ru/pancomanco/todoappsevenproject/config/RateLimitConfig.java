package ru.pancomanco.todoappsevenproject.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.pancomanco.todoappsevenproject.properties.RateLimitProperties;

import java.time.Duration;

@Configuration
@Profile("!test")
public class RateLimitConfig {

    public static final BucketConfiguration REGISTER_IP_LIMIT = buildConfig(5, Duration.ofMinutes(10));
    public static final BucketConfiguration REGISTER_EMAIL_LIMIT = buildConfig(3, Duration.ofHours(1));

    public static final BucketConfiguration LOGIN_IP_LIMIT = buildConfig(20, Duration.ofMinutes(1));
    public static final BucketConfiguration LOGIN_EMAIL_LIMIT = buildConfig(5, Duration.ofMinutes(1));
    public static final BucketConfiguration LOGIN_IP_EMAIL_LIMIT = buildConfig(10, Duration.ofMinutes(1));

    public static final BucketConfiguration VERIFY_EMAIL_IP_LIMIT = buildConfig(30, Duration.ofMinutes(10));
    public static final BucketConfiguration VERIFY_EMAIL_EMAIL_LIMIT = buildConfig(10, Duration.ofMinutes(10));

    public static final BucketConfiguration RESEND_VERIFICATION_IP_LIMIT = buildConfig(5, Duration.ofMinutes(10));
    public static final BucketConfiguration RESEND_VERIFICATION_EMAIL_LIMIT = buildConfig(2, Duration.ofMinutes(1));
    public static final BucketConfiguration RESEND_VERIFICATION_EMAIL_HOUR_LIMIT = buildConfig(5, Duration.ofHours(1));

    public static final BucketConfiguration FORGOT_PASSWORD_IP_LIMIT = buildConfig(10, Duration.ofMinutes(15));
    public static final BucketConfiguration FORGOT_PASSWORD_EMAIL_LIMIT = buildConfig(3, Duration.ofMinutes(15));
    public static final BucketConfiguration FORGOT_PASSWORD_EMAIL_HOUR_LIMIT = buildConfig(5, Duration.ofHours(1));

    public static final BucketConfiguration RESET_PASSWORD_IP_LIMIT = buildConfig(10, Duration.ofMinutes(10));
    public static final BucketConfiguration RESET_PASSWORD_TOKEN_LIMIT = buildConfig(5, Duration.ofMinutes(10));

    private static BucketConfiguration buildConfig(long capacity, Duration refillPeriod) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillPeriod)
                        .build())
                .build();
    }


    @Bean(destroyMethod = "shutdown")
    RedisClient bucket4jRedisClient(
            RateLimitProperties rateLimitProperties
    ) {
        return RedisClient.create("redis://" + rateLimitProperties.host() + ":" + rateLimitProperties.port());
    }

    @Bean(destroyMethod = "close")
    StatefulRedisConnection<byte[], byte[]> bucket4jRedisConnection(RedisClient bucket4jRedisClient) {
        return bucket4jRedisClient.connect(ByteArrayCodec.INSTANCE);
    }

    @Bean
    ProxyManager<byte[]> bucket4jProxyManager(
            StatefulRedisConnection<byte[], byte[]> bucket4jRedisConnection
    ) {
        return Bucket4jLettuce.casBasedBuilder(bucket4jRedisConnection)
                .expirationAfterWrite(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofHours(24)
                        )
                )
                .build();
    }
}
