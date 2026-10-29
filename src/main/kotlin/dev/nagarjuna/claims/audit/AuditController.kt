package dev.nagarjuna.claims.audit

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/audit")
@Tag(name = "Audit")
class AuditController(private val audit: AuditLog) {

    @GetMapping("/verify")
    @PreAuthorize("hasRole('AUDITOR')")
    @Operation(summary = "Check the audit log's hash chain", description = "Auditor role. Reports the first entry that doesn't match.")
    fun verify(): AuditVerification = audit.verify()
}
