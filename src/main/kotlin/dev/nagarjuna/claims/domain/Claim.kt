package dev.nagarjuna.claims.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class ClaimStatus { SUBMITTED, APPROVED, REJECTED }

@Entity
@Table(name = "claims")
class Claim(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val ownerId: String,

    @Column(nullable = false)
    val ownerName: String,

    @Column(nullable = false)
    val description: String,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false)
    val submittedAt: Instant = Instant.now(),
) {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ClaimStatus = ClaimStatus.SUBMITTED
        private set

    var decidedBy: String? = null
        private set

    var decidedAt: Instant? = null
        private set

    fun decide(approved: Boolean, managerName: String) {
        check(status == ClaimStatus.SUBMITTED) { "Claim $id was already ${status.name.lowercase()}" }
        status = if (approved) ClaimStatus.APPROVED else ClaimStatus.REJECTED
        decidedBy = managerName
        decidedAt = Instant.now()
    }
}
