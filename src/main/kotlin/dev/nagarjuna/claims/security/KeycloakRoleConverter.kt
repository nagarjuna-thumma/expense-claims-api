package dev.nagarjuna.claims.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

/**
 * Keycloak puts realm roles under realm_access.roles rather than in the scope claim that
 * Spring reads by default. This maps them to ROLE_ authorities, e.g. "manager" -> ROLE_MANAGER.
 */
class KeycloakRoleConverter : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val realmAccess = jwt.getClaimAsMap("realm_access") ?: emptyMap()
        val roles = (realmAccess["roles"] as? Collection<*>).orEmpty().filterIsInstance<String>()
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_${it.uppercase()}") }
        return JwtAuthenticationToken(jwt, authorities, jwt.subject)
    }
}
