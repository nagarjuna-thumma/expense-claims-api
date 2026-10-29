package dev.nagarjuna.claims.api

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The spec is generated from the controllers at /v3/api-docs, with Swagger UI at /swagger-ui.html.
 * A copy is committed in docs/openapi.json, and OpenApiContractTest fails if the two drift apart,
 * so an API change always shows up in review as a spec diff.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Expense claims API")
                .version("0.1.0")
                .description("Employees submit expense claims, managers approve or reject them, auditors verify the audit log. All calls need a Keycloak-issued JWT."),
        )
        .servers(listOf(Server().url("http://localhost:8080").description("Local")))
        .components(
            Components().addSecuritySchemes(
                BEARER,
                SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"),
            ),
        )
        .addSecurityItem(SecurityRequirement().addList(BEARER))

    companion object {
        const val BEARER = "keycloak-jwt"
    }
}
