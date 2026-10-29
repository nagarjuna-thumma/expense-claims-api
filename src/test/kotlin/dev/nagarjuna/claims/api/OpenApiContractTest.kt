package dev.nagarjuna.claims.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import dev.nagarjuna.claims.TestcontainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Files
import java.nio.file.Path

/**
 * docs/openapi.json is the published contract (the Angular client is typed against it).
 * If a controller change alters the API, this fails until the file is regenerated with
 * `UPDATE_OPENAPI=true ./gradlew test`, so the change shows up as a spec diff in review.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
@Testcontainers(disabledWithoutDocker = true)
class OpenApiContractTest(@Autowired val mvc: MockMvc) {

    private val json = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT, SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    private val committed = Path.of("docs/openapi.json")

    @Test
    fun `served spec matches the committed contract`() {
        val body = mvc.get("/v3/api-docs").andExpect { status { isOk() } }.andReturn().response.contentAsString
        val served = json.writeValueAsString(json.readValue(body, Map::class.java)) + "\n"

        if (System.getenv("UPDATE_OPENAPI") == "true") Files.writeString(committed, served)

        assertThat(Files.exists(committed)).`as`("docs/openapi.json exists").isTrue()
        assertThat(served).`as`("API changed: regenerate with UPDATE_OPENAPI=true ./gradlew test")
            .isEqualTo(Files.readString(committed))
    }

    @Test
    fun `spec is public but declares that every operation needs a bearer token`() {
        mvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.components.securitySchemes.keycloak-jwt.scheme") { value("bearer") }
            jsonPath("$.security[0].keycloak-jwt") { exists() }
            jsonPath("$.paths['/claims/{id}/approve'].post.responses['409']") { exists() }
        }
    }
}
