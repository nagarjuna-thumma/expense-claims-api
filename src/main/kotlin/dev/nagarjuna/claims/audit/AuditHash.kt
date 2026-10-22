package dev.nagarjuna.claims.audit

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.time.Instant
import java.util.HexFormat
import java.util.UUID

object AuditHash {

    /** prev_hash of the first entry. */
    val GENESIS = "0".repeat(64)

    /**
     * SHA-256 over every field of the entry plus the previous entry's hash.
     *
     * Values are written in a fixed text form (amount with two decimals, time in ISO-8601 at
     * microsecond precision, which is what Postgres stores) so the hash comes out the same
     * after a round trip through the database.
     */
    fun of(
        seq: Long,
        claimId: UUID,
        action: AuditAction,
        actorId: String,
        actorName: String,
        amount: BigDecimal,
        occurredAt: Instant,
        prevHash: String,
    ): String {
        val canonical = listOf(
            seq.toString(),
            claimId.toString(),
            action.name,
            actorId,
            actorName,
            amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
            occurredAt.toString(),
            prevHash,
        ).joinToString("\n")
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return HexFormat.of().formatHex(digest)
    }
}
