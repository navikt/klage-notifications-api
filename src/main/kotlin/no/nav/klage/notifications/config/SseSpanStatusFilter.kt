package no.nav.klage.notifications.config

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.klage.notifications.util.getLogger
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Marks the current OpenTelemetry span as `OK` when an exception thrown from the
 * SSE notification endpoint looks like a client disconnect (Broken pipe,
 * AsyncRequestNotUsableException, EofException, ClientAbortException, etc.).
 *
 * Rationale: clients of long-lived SSE streams disconnect all the time (page
 * refresh, navigation, network blip, tab close...). The OTEL Java agent's
 * servlet instrumentation otherwise records these as server-side errors, which
 * pollutes traces, dashboards and alerts.
 *
 * Per the OTEL spec (`Span#setStatus`): once a span's status is set to `OK` it
 * is final and any further attempts to change it MUST be ignored. This means
 * that even if the agent later tries to set `ERROR` (because of the propagating
 * exception or a 5xx response status) it will be a no-op and the span will be
 * exported with status `OK`.
 *
 * The filter is registered with the highest precedence so that it sits outside
 * any other filter and can observe exceptions thrown anywhere in the chain. It
 * intentionally only activates for the SSE endpoint to avoid masking genuine
 * errors elsewhere, and runs on `REQUEST` and `ASYNC` dispatches (the SSE
 * stream is served through the async dispatcher).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SseSpanStatusFilter : OncePerRequestFilter() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val ourLogger = getLogger(javaClass.enclosingClass)

        private const val SSE_PATH = "/user/notifications/events"

        // Substrings of exception messages / class names that indicate a client
        // disconnect rather than a real server-side error.
        private val DISCONNECT_INDICATORS = listOf(
            "Broken pipe",
            "Connection reset by peer",
            "ClientAbortException",
            "AsyncRequestNotUsableException",
            "EofException",
            "Response already committed",
        )
    }

    /** Run on async dispatches too — that's where SSE streaming actually happens. */
    override fun shouldNotFilterAsyncDispatch(): Boolean = false

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // Only act on the SSE endpoint; never interfere with anything else.
        return !request.requestURI.contains(SSE_PATH)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            if (looksLikeClientDisconnect(e)) {
                markSpanOk(e, request.dispatcherType)
            }
            throw e
        }
    }

    private fun markSpanOk(e: Throwable, dispatcherType: DispatcherType) {
        val span = Span.current()
        // No-op span has an invalid SpanContext; nothing useful to do then.
        if (!span.spanContext.isValid) {
            ourLogger.debug(
                "No active OTEL span when handling client disconnect on SSE endpoint " +
                    "(dispatcherType={}, exception={})",
                dispatcherType,
                e.toString(),
            )
            return
        }
        span.setStatus(StatusCode.OK, "Client disconnected (expected for SSE)")
        ourLogger.debug(
            "Marked SSE span as OK after client disconnect (dispatcherType={}): {}",
            dispatcherType,
            e.toString(),
        )
    }

    private fun looksLikeClientDisconnect(t: Throwable): Boolean {
        var cause: Throwable? = t
        var depth = 0
        while (cause != null && depth < 16) {
            val message = cause.message
            val typeName = cause.javaClass.name
            if (DISCONNECT_INDICATORS.any { indicator ->
                    message?.contains(indicator, ignoreCase = true) == true ||
                        typeName.contains(indicator, ignoreCase = true)
                }
            ) {
                return true
            }
            cause = cause.cause
            depth++
        }
        return false
    }
}
