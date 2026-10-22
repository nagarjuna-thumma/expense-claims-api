package dev.nagarjuna.claims.audit

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuditRepository : JpaRepository<AuditEntry, Long> {
    fun findTopByOrderBySeqDesc(): AuditEntry?
    fun findByClaimIdOrderBySeq(claimId: UUID): List<AuditEntry>
    fun findTop500BySeqGreaterThanOrderBySeq(seq: Long): List<AuditEntry>
}
