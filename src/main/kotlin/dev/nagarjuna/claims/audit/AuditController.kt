package dev.nagarjuna.claims.audit

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/audit")
class AuditController(private val audit: AuditLog) {

    @GetMapping("/verify")
    @PreAuthorize("hasRole('AUDITOR')")
    fun verify(): AuditVerification = audit.verify()
}
