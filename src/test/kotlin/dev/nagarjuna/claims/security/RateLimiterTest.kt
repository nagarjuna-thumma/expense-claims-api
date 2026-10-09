package dev.nagarjuna.claims.security

import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RateLimiterTest {

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant() = now
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId) = this
    }

    private val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))
    private val limiter = RateLimiter(capacity = 3, period = Duration.ofSeconds(30), clock = clock)

    @Test
    fun `allows up to capacity then asks the caller to wait`() {
        repeat(3) { assertEquals(0, limiter.tryAcquire("alice")) }

        assertTrue(limiter.tryAcquire("alice") > 0)
    }

    @Test
    fun `each user has their own bucket`() {
        repeat(3) { limiter.tryAcquire("alice") }

        assertEquals(0, limiter.tryAcquire("bob"))
    }

    @Test
    fun `tokens come back as time passes`() {
        repeat(3) { limiter.tryAcquire("alice") }

        clock.now = clock.now.plusSeconds(10) // one token per 10 seconds

        assertEquals(0, limiter.tryAcquire("alice"))
        assertTrue(limiter.tryAcquire("alice") > 0)
    }
}
