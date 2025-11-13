package no.nav.klage.notifications.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

fun getLogger(forClass: Class<*>): Logger = LoggerFactory.getLogger(forClass)

fun getTeamLogger(): Logger = LoggerFactory.getLogger("team-logs")

fun logErrorResponse(
    response: ClientResponse,
    functionName: String,
    classLogger: Logger,
): Mono<WebClientResponseException> {
    val errorString = "Got ${response.statusCode()} when requesting $functionName"
    return response.createException()
        .doOnNext { ex ->
            classLogger.error("$errorString. See team-logs for more details.")
            getTeamLogger().error("$errorString - response body: '${ex.responseBodyAsString}'")
        }
}