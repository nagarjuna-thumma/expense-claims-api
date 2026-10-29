package dev.nagarjuna.claims.api

import dev.nagarjuna.claims.audit.AuditEntry
import dev.nagarjuna.claims.domain.Claim
import dev.nagarjuna.claims.domain.ClaimService
import dev.nagarjuna.claims.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.util.UUID

data class SubmitClaimRequest(
    @field:NotBlank @field:Size(max = 200) val description: String,
    @field:DecimalMin("0.01") @field:DecimalMax("10000.00") val amount: BigDecimal,
)

data class ClaimResponse(
    val id: UUID,
    val owner: String,
    val description: String,
    val amount: BigDecimal,
    val status: String,
    val submittedAt: Instant,
    val decidedBy: String?,
) {
    companion object {
        fun from(c: Claim) = ClaimResponse(c.id, c.ownerName, c.description, c.amount, c.status.name, c.submittedAt, c.decidedBy)
    }
}

data class HistoryEntry(val seq: Long, val action: String, val actor: String, val amount: BigDecimal, val at: Instant) {
    companion object {
        fun from(e: AuditEntry) = HistoryEntry(e.seq, e.action.name, e.actorName, e.amount, e.occurredAt)
    }
}

@RestController
@RequestMapping("/claims")
@Tag(name = "Claims")
class ClaimController(private val service: ClaimService) {

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Submit a claim", description = "Employee role.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Missing description or amount outside 0.01 to 10,000")
    fun submit(auth: JwtAuthenticationToken, @Valid @RequestBody request: SubmitClaimRequest): ResponseEntity<ClaimResponse> {
        val claim = service.submit(CurrentUser.from(auth), request.description, request.amount)
        return ResponseEntity.created(URI.create("/claims/${claim.id}")).body(ClaimResponse.from(claim))
    }

    @GetMapping("/mine")
    @Operation(summary = "List my claims")
    fun mine(auth: JwtAuthenticationToken): List<ClaimResponse> =
        service.mine(CurrentUser.from(auth)).map(ClaimResponse::from)

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "List claims waiting for a decision", description = "Manager role.")
    fun pending(): List<ClaimResponse> = service.pending().map(ClaimResponse::from)

    @GetMapping("/{id}")
    @Operation(summary = "Get one claim", description = "Owners see their own claims, managers see any claim.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "No such claim, or it belongs to someone else. Same answer either way, so ids can't be probed.")
    fun get(auth: JwtAuthenticationToken, @PathVariable id: UUID): ClaimResponse =
        ClaimResponse.from(service.get(CurrentUser.from(auth), id))

    @GetMapping("/{id}/history")
    @Operation(summary = "Audit trail of one claim", description = "Same visibility rules as getting the claim.")
    fun history(auth: JwtAuthenticationToken, @PathVariable id: UUID): List<HistoryEntry> =
        service.history(CurrentUser.from(auth), id).map(HistoryEntry::from)

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Approve a claim", description = "Manager role. Managers can't approve their own claims.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "403", description = "It's your own claim")
    @ApiResponse(responseCode = "409", description = "Already decided")
    fun approve(auth: JwtAuthenticationToken, @PathVariable id: UUID): ClaimResponse =
        ClaimResponse.from(service.decide(CurrentUser.from(auth), id, approve = true))

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Reject a claim", description = "Manager role.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "409", description = "Already decided")
    fun reject(auth: JwtAuthenticationToken, @PathVariable id: UUID): ClaimResponse =
        ClaimResponse.from(service.decide(CurrentUser.from(auth), id, approve = false))
}
