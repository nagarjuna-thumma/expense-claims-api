package dev.nagarjuna.claims.security

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

data class CurrentUser(val id: String, val name: String, val isManager: Boolean) {
    companion object {
        fun from(auth: JwtAuthenticationToken) = CurrentUser(
            id = auth.token.subject,
            name = auth.token.getClaimAsString("preferred_username") ?: auth.token.subject,
            isManager = auth.authorities.any { it.authority == "ROLE_MANAGER" },
        )
    }
}
