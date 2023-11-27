package com.tuneit.coursesshopreactive.filters

import com.tuneit.coursesshopreactive.model.RateLimitException
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.Refill
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import org.springframework.boot.web.reactive.filter.OrderedWebFilter
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import org.springframework.web.util.pattern.PathPattern
import org.springframework.web.util.pattern.PathPatternParser
import reactor.core.publisher.Mono
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.TimeUnit


@Component
class RateLimitFilter (val lettuceProxyManager: LettuceBasedProxyManager): OrderedWebFilter {

    private fun getBucketConfig(): BucketConfiguration {
        val conf = BucketConfiguration.builder()
        conf.addLimit(Bandwidth.classic(50, Refill.intervally(50, Duration.ofHours(1))))
        conf.addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
        return conf.build()
    }

    private fun getBucketKeyForRemoteAddr(request: ServerHttpRequest): ByteArray {
        val ipFromHeader: String? = request.headers.getFirst("X-FORWARDED-FOR")
        val ip = if (ipFromHeader.isNullOrBlank()) request.remoteAddress.toString() else ipFromHeader
        return MessageDigest.getInstance("SHA-256").digest(ip.toByteArray())
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val bucketKey = getBucketKeyForRemoteAddr(request)
        val bucket: AsyncBucketProxy = lettuceProxyManager.asAsync()
                                                         .builder()
                                                         .build(bucketKey, getBucketConfig())
        if(urlMatches(request)) {
          return Mono
                .fromFuture(bucket.tryConsumeAndReturnRemaining(1))
                .flatMap { handleConsumptionProbe(exchange, chain, it) }
        }
        return chain.filter(exchange)
    }

    val pathsToFilter: List<PathPattern> =
        listOf(PathPatternParser.defaultInstance.parse("/courses"),
               PathPatternParser.defaultInstance.parse("/courses/{id}"))
    
    private fun urlMatches(request: ServerHttpRequest) : Boolean {
        return pathsToFilter.any { it.matches(request.path.pathWithinApplication())}
               && request.method.matches(HttpMethod.GET.name())
    }

    private fun handleConsumptionProbe(exchange: ServerWebExchange,
                                       chain: WebFilterChain,
                                       probe: ConsumptionProbe) : Mono<Void>  {
        val response = exchange.response
        if(probe.isConsumed) {
           response.headers.set("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
           return chain.filter(exchange)
        }
        response.headers.set("X-Rate-Limit-Retry-After-Seconds",
                             TimeUnit.NANOSECONDS.toSeconds(probe.nanosToWaitForRefill).toString())
        return Mono.error(RateLimitException("Too many requests"))
    }

    override fun getOrder(): Int {
        return 1;
    }
}
