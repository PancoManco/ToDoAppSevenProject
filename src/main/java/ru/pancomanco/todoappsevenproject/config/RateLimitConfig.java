package ru.pancomanco.todoappsevenproject.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.pancomanco.todoappsevenproject.properties.RateLimitProperties;

import java.time.Duration;

@Configuration
public class RateLimitConfig {
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
