package dev.nagarjuna.claims.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import java.time.Duration

@ConfigurationProperties("rate-limit")
data class RateLimitProperties(val requests: Int = 30, val period: Duration = Duration.ofMinutes(1))

@Configuration
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity, rateLimit: RateLimitProperties): SecurityFilterChain {
        http
            .csrf { it.disable() } // stateless API with bearer tokens, no cookies to protect
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                // The spec only describes the API. Every call it lists still needs a token.
                it.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt { jwt -> jwt.jwtAuthenticationConverter(KeycloakRoleConverter()) } }
            // Created here rather than as a bean, so Boot doesn't also register it as a servlet filter.
            .addFilterAfter(
                RateLimitFilter(RateLimiter(rateLimit.requests, rateLimit.period)),
                BearerTokenAuthenticationFilter::class.java,
            )
        return http.build()
    }
}
