package dev.nagarjuna.claims.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Runs after the bearer token is checked, so limits are per user (JWT subject), not per IP.
 */
class RateLimitFilter(private val limiter: RateLimiter) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val user = SecurityContextHolder.getContext().authentication?.name
        if (user == null) {
            chain.doFilter(request, response)
            return
        }
        val waitSeconds = limiter.tryAcquire(user)
        if (waitSeconds == 0L) {
            chain.doFilter(request, response)
            return
        }
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.setHeader("Retry-After", waitSeconds.toString())
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.writer.write("""{"title":"Too Many Requests","status":429,"detail":"Slow down and retry in $waitSeconds s"}""")
    }
}
