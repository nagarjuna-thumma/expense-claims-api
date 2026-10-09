package dev.nagarjuna.claims.security

import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * A simple token bucket per key. Each key gets [capacity] requests, refilled evenly over [period].
 *
 * Buckets live in memory, so the limit is per instance. Running several instances behind a load
 * balancer would need a shared store such as Redis instead.
 */
class RateLimiter(
    private val capacity: Int,
    private val period: Duration,
    private val clock: Clock = Clock.systemUTC(),
) {
    private class Bucket(var tokens: Double, var lastRefillNanos: Long)

    private val buckets = ConcurrentHashMap<String, Bucket>()
    private val refillPerNano = capacity.toDouble() / period.toNanos()

    /** Returns 0 if the request is allowed, otherwise how many seconds to wait. */
    fun tryAcquire(key: String): Long {
        val now = nowNanos()
        val bucket = buckets.computeIfAbsent(key) { Bucket(capacity.toDouble(), now) }
        synchronized(bucket) {
            bucket.tokens = minOf(capacity.toDouble(), bucket.tokens + (now - bucket.lastRefillNanos) * refillPerNano)
            bucket.lastRefillNanos = now
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1
                return 0
            }
            val nanosUntilNextToken = (1 - bucket.tokens) / refillPerNano
            return maxOf(1, Duration.ofNanos(nanosUntilNextToken.toLong()).toSeconds() + 1)
        }
    }

    private fun nowNanos(): Long {
        val instant = clock.instant()
        return instant.epochSecond * 1_000_000_000 + instant.nano
    }
}
