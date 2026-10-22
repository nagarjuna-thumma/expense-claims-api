package dev.nagarjuna.claims.audit

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AuditHashTest {

    private val claimId = UUID.fromString("7d6f6e3a-1c2b-4d5e-8f90-123456789abc")
    private val at = Instant.parse("2026-10-22T14:03:07.123456Z")

    private fun hash(
        seq: Long = 1,
        action: AuditAction = AuditAction.APPROVED,
        actor: String = "bob",
        amount: String = "42.50",
        prev: String = AuditHash.GENESIS,
    ) = AuditHash.of(seq, claimId, action, actor, actor, BigDecimal(amount), at, prev)

    @Test
    fun `same entry always gives the same hash`() {
        assertEquals(hash(), hash())
        assertEquals(64, hash().length)
    }

    @Test
    fun `amount scale does not change the hash`() {
        // Postgres hands back 42.50 even if 42.5 went in, so both must hash the same.
        assertEquals(hash(amount = "42.5"), hash(amount = "42.50"))
    }

    @Test
    fun `changing any field changes the hash`() {
        val original = hash()
        assertNotEquals(original, hash(seq = 2))
        assertNotEquals(original, hash(action = AuditAction.REJECTED))
        assertNotEquals(original, hash(actor = "mallory"))
        assertNotEquals(original, hash(amount = "4250.00"))
        assertNotEquals(original, hash(prev = "f".repeat(64)))
    }
}
