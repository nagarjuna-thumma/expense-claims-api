package dev.nagarjuna.claims.domain

import dev.nagarjuna.claims.security.CurrentUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

class ClaimNotFoundException(id: UUID) : RuntimeException("Claim $id not found")
class SelfApprovalException : RuntimeException("Managers can't decide on their own claims")

@Service
class ClaimService(private val claims: ClaimRepository) {

    @Transactional
    fun submit(user: CurrentUser, description: String, amount: BigDecimal): Claim =
        claims.save(Claim(ownerId = user.id, ownerName = user.name, description = description, amount = amount))

    @Transactional(readOnly = true)
    fun mine(user: CurrentUser): List<Claim> = claims.findByOwnerIdOrderBySubmittedAtDesc(user.id)

    @Transactional(readOnly = true)
    fun pending(): List<Claim> = claims.findByStatusOrderBySubmittedAt(ClaimStatus.SUBMITTED)

    /**
     * Owners can see their own claims and managers can see any claim. Anyone else gets a
     * "not found" rather than "forbidden", so the API doesn't confirm which ids exist.
     */
    @Transactional(readOnly = true)
    fun get(user: CurrentUser, id: UUID): Claim {
        val claim = claims.findById(id).orElseThrow { ClaimNotFoundException(id) }
        if (claim.ownerId != user.id && !user.isManager) throw ClaimNotFoundException(id)
        return claim
    }

    @Transactional
    fun decide(manager: CurrentUser, id: UUID, approve: Boolean): Claim {
        val claim = claims.findById(id).orElseThrow { ClaimNotFoundException(id) }
        if (claim.ownerId == manager.id) throw SelfApprovalException()
        claim.decide(approve, manager.name)
        return claim
    }
}
