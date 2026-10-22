package dev.nagarjuna.claims.audit

import dev.nagarjuna.claims.domain.Claim
import dev.nagarjuna.claims.security.CurrentUser
import jakarta.persistence.EntityManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.temporal.ChronoUnit
import java.util.UUID

data class AuditVerification(val entries: Long, val valid: Boolean, val firstBadSeq: Long? = null, val problem: String? = null)

@Component
class AuditLog(
    private val entries: AuditRepository,
    private val em: EntityManager,
    private val jdbc: JdbcTemplate,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager,
) {
    private val ownTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /**
     * Appends an entry in the caller's transaction, so the entry and the change it describes
     * commit or roll back together. There is never a decision without an audit row, or the reverse.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    fun record(claim: Claim, action: AuditAction, actor: CurrentUser): AuditEntry = append(claim, action, actor)

    /**
     * For refused actions. The refusal rolls back the caller's transaction, but the attempt
     * still has to be on record, so it is written in a transaction of its own.
     */
    fun recordRefusal(claim: Claim, action: AuditAction, actor: CurrentUser): AuditEntry =
        ownTransaction.execute { append(claim, action, actor) }!!

    @Transactional(readOnly = true)
    fun history(claimId: UUID): List<AuditEntry> = entries.findByClaimIdOrderBySeq(claimId)

    /** Walks the whole chain and reports the first entry that doesn't check out. */
    @Transactional(readOnly = true)
    fun verify(): AuditVerification {
        var expectedSeq = 1L
        var prevHash = AuditHash.GENESIS
        while (true) {
            val page = entries.findTop500BySeqGreaterThanOrderBySeq(expectedSeq - 1)
            if (page.isEmpty()) return AuditVerification(expectedSeq - 1, valid = true)
            for (entry in page) {
                val problem = when {
                    entry.seq != expectedSeq -> "expected entry $expectedSeq but found ${entry.seq}"
                    entry.prevHash != prevHash -> "prev_hash does not match the entry before it"
                    entry.hash != entry.computeHash() -> "contents do not match the stored hash"
                    else -> null
                }
                if (problem != null) {
                    return AuditVerification(expectedSeq - 1, valid = false, firstBadSeq = expectedSeq, problem = problem)
                }
                prevHash = entry.hash
                expectedSeq++
            }
        }
    }

    private fun append(claim: Claim, action: AuditAction, actor: CurrentUser): AuditEntry {
        // One writer at a time: each entry needs the previous entry's hash. The lock is released at commit.
        jdbc.execute("SELECT pg_advisory_xact_lock($CHAIN_LOCK_ID)")
        val last = entries.findTopByOrderBySeqDesc()
        val seq = (last?.seq ?: 0) + 1
        val prevHash = last?.hash ?: AuditHash.GENESIS
        val now = clock.instant().truncatedTo(ChronoUnit.MICROS)
        val hash = AuditHash.of(seq, claim.id, action, actor.id, actor.name, claim.amount, now, prevHash)
        val entry = AuditEntry(seq, claim.id, action, actor.id, actor.name, claim.amount, now, prevHash, hash)
        em.persist(entry)
        return entry
    }

    private companion object {
        const val CHAIN_LOCK_ID = 7_243_001L
    }
}
