package dev.nagarjuna.claims.api

import dev.nagarjuna.claims.domain.ClaimNotFoundException
import dev.nagarjuna.claims.domain.SelfApprovalException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ErrorHandler {

    @ExceptionHandler(ClaimNotFoundException::class)
    fun notFound(e: ClaimNotFoundException) = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message)

    @ExceptionHandler(SelfApprovalException::class)
    fun selfApproval(e: SelfApprovalException) = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.message)

    @ExceptionHandler(IllegalStateException::class)
    fun conflict(e: IllegalStateException) = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.message)
}
