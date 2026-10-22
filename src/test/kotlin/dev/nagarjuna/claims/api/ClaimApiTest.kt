package dev.nagarjuna.claims.api

import dev.nagarjuna.claims.TestcontainersConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.junit.jupiter.Testcontainers
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Full application against a real Postgres, with JWTs created by spring-security-test
 * instead of a live Keycloak. KeycloakIntegrationTest covers the real token path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = ["rate-limit.requests=1000"])
class ClaimApiTest(
    @Autowired val mvc: MockMvc,
    @Autowired val json: ObjectMapper,
) {

    private fun user(id: String, vararg roles: String) = jwt()
        .jwt { it.subject(id).claim("preferred_username", id) }
        .authorities(roles.map { SimpleGrantedAuthority("ROLE_$it") })

    private val alice = user("alice", "EMPLOYEE")
    private val carol = user("carol", "EMPLOYEE")
    private val bob = user("bob", "EMPLOYEE", "MANAGER")
    private val dave = user("dave", "AUDITOR")

    private fun submit(as_: org.springframework.test.web.servlet.request.RequestPostProcessor, amount: String = "42.50"): String {
        val body = mvc.post("/claims") {
            with(as_)
            contentType = MediaType.APPLICATION_JSON
            content = """{"description":"Taxi to client site","amount":$amount}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return json.readTree(body)["id"].asText()
    }

    @Test
    fun `requests without a token are rejected`() {
        mvc.get("/claims/mine").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `employee submits a claim and sees it in their list`() {
        val id = submit(alice)

        mvc.get("/claims/mine") { with(alice) }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.id == '$id')].status") { value("SUBMITTED") }
        }
    }

    @Test
    fun `another employee cannot see someone else's claim`() {
        val id = submit(alice)

        mvc.get("/claims/$id") { with(carol) }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `manager can see and approve an employee's claim`() {
        val id = submit(alice)

        mvc.get("/claims/$id") { with(bob) }.andExpect { status { isOk() } }
        mvc.post("/claims/$id/approve") { with(bob) }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("APPROVED") }
            jsonPath("$.decidedBy") { value("bob") }
        }
    }

    @Test
    fun `employees cannot approve claims`() {
        val id = submit(alice)

        mvc.post("/claims/$id/approve") { with(carol) }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `manager cannot approve their own claim`() {
        val id = submit(bob)

        mvc.post("/claims/$id/approve") { with(bob) }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `a decided claim cannot be decided again`() {
        val id = submit(alice)
        mvc.post("/claims/$id/reject") { with(bob) }.andExpect { status { isOk() } }

        mvc.post("/claims/$id/approve") { with(bob) }.andExpect { status { isConflict() } }
    }

    @Test
    fun `invalid amount is rejected`() {
        mvc.post("/claims") {
            with(alice)
            contentType = MediaType.APPLICATION_JSON
            content = """{"description":"Too much","amount":-5}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `claim history shows who did what`() {
        val id = submit(alice)
        mvc.post("/claims/$id/approve") { with(bob) }.andExpect { status { isOk() } }

        mvc.get("/claims/$id/history") { with(alice) }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].action") { value("SUBMITTED") }
            jsonPath("$[0].actor") { value("alice") }
            jsonPath("$[1].action") { value("APPROVED") }
            jsonPath("$[1].actor") { value("bob") }
        }
    }

    @Test
    fun `a blocked self-approval is still recorded`() {
        val id = submit(bob)

        mvc.post("/claims/$id/approve") { with(bob) }.andExpect { status { isForbidden() } }

        mvc.get("/claims/$id/history") { with(bob) }.andExpect {
            jsonPath("$[1].action") { value("SELF_APPROVAL_BLOCKED") }
            jsonPath("$[1].actor") { value("bob") }
        }
        mvc.get("/claims/$id") { with(bob) }.andExpect { jsonPath("$.status") { value("SUBMITTED") } }
    }

    @Test
    fun `history follows the same visibility rule as the claim`() {
        val id = submit(alice)

        mvc.get("/claims/$id/history") { with(carol) }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `only auditors can verify the audit log`() {
        submit(alice)

        mvc.get("/audit/verify") { with(bob) }.andExpect { status { isForbidden() } }
        mvc.get("/audit/verify") { with(dave) }.andExpect {
            status { isOk() }
            jsonPath("$.valid") { value(true) }
        }
    }

    @Test
    fun `auditors cannot submit or decide claims`() {
        val id = submit(alice)

        mvc.post("/claims") {
            with(dave)
            contentType = MediaType.APPLICATION_JSON
            content = """{"description":"Lunch","amount":12}"""
        }.andExpect { status { isForbidden() } }
        mvc.post("/claims/$id/approve") { with(dave) }.andExpect { status { isForbidden() } }
    }
}
