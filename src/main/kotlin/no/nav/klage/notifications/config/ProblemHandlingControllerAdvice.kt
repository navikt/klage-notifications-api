package no.nav.klage.notifications.config

import no.nav.klage.notifications.exceptions.MissingAccessException
import no.nav.klage.notifications.exceptions.NotificationNotFoundException
import no.nav.klage.notifications.util.getLogger
import no.nav.klage.notifications.util.getTeamLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class ProblemHandlingControllerAdvice : ResponseEntityExceptionHandler() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val ourLogger = getLogger(javaClass.enclosingClass)
        private val teamLogger = getTeamLogger()
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        logError(
            httpStatus = HttpStatus.valueOf(statusCode.value()),
            errorMessage = ex.message ?: "No error message available",
            exception = ex,
        )

        return super.handleExceptionInternal(ex, body, headers, statusCode, request)
    }

    //add handling for notification not found
    @ExceptionHandler
    fun handleNotificationNotFound(ex: NotificationNotFoundException): ProblemDetail =
        create(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler
    fun handleEntityNotFound(
        ex: JpaObjectRetrievalFailureException,
    ): ProblemDetail =
        create(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler
    fun handleEntityNotFound(
        ex: NoSuchElementException,
    ): ProblemDetail =
        create(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler
    fun handleMissingAccess(ex: MissingAccessException): ProblemDetail =
        create(HttpStatus.FORBIDDEN, ex)

    @ExceptionHandler
    fun handleResponseStatusException(ex: WebClientResponseException): ResponseEntity<Any> =
        createProblemForWebClientResponseException(ex)

    @ExceptionHandler
    fun handleIllegalStateException(
        ex: IllegalStateException,
    ): ProblemDetail =
        create(HttpStatus.BAD_REQUEST, ex)

    private fun createProblemForWebClientResponseException(ex: WebClientResponseException): ResponseEntity<Any> {
        logError(
            httpStatus = HttpStatus.valueOf(ex.statusCode.value()),
            errorMessage = ex.statusText,
            exception = ex
        )

        val contentType = ex.headers.contentType
        if (contentType != null && MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(contentType)) {
            // Pass through as-is when upstream already returned problem+json
            val body = ex.responseBodyAsByteArray
            return ResponseEntity.status(ex.statusCode).contentType(contentType).body(body)
        }

        // Fallback: wrap into a ProblemDetail
        val problemDetail = ProblemDetail.forStatus(ex.statusCode).apply {
            title = ex.statusText
            detail = ex.responseBodyAsString
        }
        return ResponseEntity
            .status(ex.statusCode)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problemDetail)
    }

    private fun create(httpStatus: HttpStatus, ex: Exception): ProblemDetail {
        val errorMessage = ex.message ?: "No error message available"

        logError(
            httpStatus = httpStatus,
            errorMessage = errorMessage,
            exception = ex
        )

        return ProblemDetail.forStatus(httpStatus).apply {
            title = errorMessage
        }
    }

    private fun logError(httpStatus: HttpStatus, errorMessage: String, exception: Exception) {
        when {
            httpStatus.is5xxServerError -> {
                ourLogger.error("Exception thrown to client: ${exception.javaClass.name}. See team-logs for more details.")
                teamLogger.error("Exception thrown to client: ${httpStatus.reasonPhrase}, $errorMessage", exception)
            }

            else -> {
                ourLogger.warn("Exception thrown to client: ${exception.javaClass.name}. See team-logs for more details.")
                teamLogger.warn("Exception thrown to client: ${httpStatus.reasonPhrase}, $errorMessage", exception)
            }
        }
    }
}