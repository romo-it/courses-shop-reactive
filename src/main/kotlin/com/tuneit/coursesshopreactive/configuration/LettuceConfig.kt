package com.tuneit.coursesshopreactive.configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class LettuceConfig (@Value("\${spring.data.redis.host}") val host : String,
                     @Value("\${spring.data.redis.port}") val port: Int) {
    
    @Bean
    fun redisClient(): RedisClient {
        return RedisClient.create(RedisURI.builder().withHost(host).withPort(port).build())
    }

    @Bean
    fun lettuceProxyManager(): LettuceBasedProxyManager {
        return LettuceBasedProxyManager.builderFor(redisClient())
                                       .withExpirationStrategy(ExpirationAfterWriteStrategy
                                       .basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
                                       .build()
    }
}