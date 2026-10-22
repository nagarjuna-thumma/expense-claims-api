package dev.nagarjuna.claims.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class AuditAction { SUBMITTED, APPROVED, REJECTED, SELF_APPROVAL_BLOCKED }

@Entity
@Immutable
@Table(name = "claim_audit_log")
class AuditEntry(
    @Id
    val seq: Long,

    @Column(nullable = false)
    val claimId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val action: AuditAction,

    @Column(nullable = false)
    val actorId: String,

    @Column(nullable = false)
    val actorName: String,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false)
    val occurredAt: Instant,

    @Column(nullable = false)
    val prevHash: String,

    @Column(nullable = false)
    val hash: String,
) {
    fun computeHash() = AuditHash.of(seq, claimId, action, actorId, actorName, amount, occurredAt, prevHash)
}
