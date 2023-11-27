package com.tuneit.coursesshopreactive.filters

import com.sletmoe.bucket4k.SuspendingBucket
import com.tuneit.coursesshopreactive.model.RateLimitException
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Refill
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.util.pattern.PathPattern
import org.springframework.web.util.pattern.PathPatternParser
import java.time.Duration
import kotlinx.coroutines.*
import org.springframework.core.Ordered
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.*
import java.security.MessageDigest
import kotlin.collections.HashMap
import kotlin.time.DurationUnit
import kotlin.time.toDuration


/*@Component*/
class CoRateLimitFilter : CoWebFilter(), Ordered {
    //A Kotlin wrapper around Bucket4j which suspends and plays nicely with coroutines.
    //https://github.com/ksletmoe/Bucket4k
    
    val localBuckets = HashMap<String, SuspendingBucket>()

    private fun createSuspendingBucket() : SuspendingBucket = SuspendingBucket.build {
        addLimit(Bandwidth.classic(50, Refill.intervally(50, Duration.ofHours(1))))
        addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
    }

    private fun getBucketKeyForRemoteAddr(request: ServerHttpRequest): String {
        val ipFromHeader: String? = request.headers.getFirst("X-FORWARDED-FOR")
        val ip = if (ipFromHeader.isNullOrBlank()) request.remoteAddress.toString() else ipFromHeader
        return MessageDigest.getInstance("SHA-256")
                            .digest(ip.toByteArray())
                            .fold(StringBuilder()) { sb, it -> sb.append("%02x".format(it)) }.toString()
    }
    
    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        val request = exchange.request
        val response = exchange.response
        val bucketKey = getBucketKeyForRemoteAddr(request)
        val localBucket = localBuckets.getOrPut(bucketKey) { createSuspendingBucket() }
        if(urlMatches(request)) {
            val isConsumed = CoroutineScope(Dispatchers.IO).async {
                localBucket.tryConsume(1, 1.toDuration(DurationUnit.SECONDS))
            }.await()
            val bucketInfo = localBucket.toString().split("[", "]")[1].split(", ")
            val availableTokensInLongPeriod = bucketInfo[1].toInt().coerceAtLeast(0)
            val availableTokensInShortPeriod = bucketInfo[4].toInt().coerceAtLeast(0)
                if(isConsumed) {
                response.headers.set("X-Rate-Limit-Remaining",
                    "$availableTokensInShortPeriod/$availableTokensInLongPeriod"
                )
                return chain.filter(exchange)
            }
            throw RateLimitException("Too many requests")
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

    override fun getOrder(): Int {
        return 1;
    }
}
