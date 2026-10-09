package dev.nagarjuna.claims.security

import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.test.assertEquals

class KeycloakRoleConverterTest {

    private fun jwt(claims: Map<String, Any>) = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("user-1")
        .claims { it.putAll(claims) }
        .build()

    @Test
    fun `realm roles become ROLE_ authorities`() {
        val auth = KeycloakRoleConverter().convert(jwt(mapOf("realm_access" to mapOf("roles" to listOf("employee", "manager")))))

        assertEquals(setOf("ROLE_EMPLOYEE", "ROLE_MANAGER"), auth.authorities.map { it.authority }.toSet())
        assertEquals("user-1", auth.name)
    }

    @Test
    fun `token without realm roles gets no authorities`() {
        val auth = KeycloakRoleConverter().convert(jwt(mapOf("scope" to "profile")))

        assertEquals(emptyList(), auth.authorities.toList())
    }
}
