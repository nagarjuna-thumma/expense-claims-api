package dev.nagarjuna.claims

import dasniko.testcontainers.keycloak.KeycloakContainer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

/**
 * End to end with a real Keycloak: log in with a password, get a signed JWT, call the API.
 * This is the test that proves token validation and role mapping work outside of mocks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig::class)
@Testcontainers(disabledWithoutDocker = true)
class KeycloakIntegrationTest(@Autowired val http: TestRestTemplate) {

    companion object {
        @Container
        @JvmStatic
        val keycloak: KeycloakContainer = KeycloakContainer("keycloak/keycloak:26.4.0")
            .withRealmImportFile("expenses-realm.json")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { issuer() }
        }

        private fun issuer() = "${keycloak.authServerUrl}/realms/expenses"
    }

    private fun login(username: String): String {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "password")
            add("client_id", "expenses-cli")
            add("username", username)
            add("password", username)
        }
        val response = RestClient.create().post()
            .uri("${issuer()}/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map::class.java)!!
        return response["access_token"] as String
    }

    private fun bearer(token: String, body: Any? = null) = HttpEntity(body, HttpHeaders().apply {
        setBearerAuth(token)
        contentType = MediaType.APPLICATION_JSON
    })

    @Test
    fun `real Keycloak tokens are accepted and roles are enforced`() {
        val alice = login("alice")
        val bob = login("bob")

        val created = http.exchange("/claims", HttpMethod.POST,
            bearer(alice, mapOf("description" to "Conference ticket", "amount" to 299.00)), Map::class.java)
        assertEquals(HttpStatus.CREATED, created.statusCode)
        val id = created.body!!["id"]

        val aliceTriesToApprove = http.exchange("/claims/$id/approve", HttpMethod.POST, bearer(alice), String::class.java)
        assertEquals(HttpStatus.FORBIDDEN, aliceTriesToApprove.statusCode)

        val bobApproves = http.exchange("/claims/$id/approve", HttpMethod.POST, bearer(bob), Map::class.java)
        assertEquals(HttpStatus.OK, bobApproves.statusCode)
        assertEquals("APPROVED", bobApproves.body!!["status"])
    }

    @Test
    fun `tampered token is rejected`() {
        val token = login("alice")
        val tampered = token.dropLast(4) + "AAAA"

        val response = http.exchange("/claims/mine", HttpMethod.GET, bearer(tampered), String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }
}
