package dev.nagarjuna.claims.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ClaimRepository : JpaRepository<Claim, UUID> {
    fun findByOwnerIdOrderBySubmittedAtDesc(ownerId: String): List<Claim>
    fun findByStatusOrderBySubmittedAt(status: ClaimStatus): List<Claim>
}
