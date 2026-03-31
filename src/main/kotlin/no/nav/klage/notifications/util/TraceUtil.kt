package no.nav.klage.notifications.util

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context

fun currentTraceparent(): String? {
    return try {
        val spanContext = Span.current().spanContext
        "00-${spanContext.traceId}-${spanContext.spanId}-${spanContext.traceFlags.asHex()}"
    } catch (_: Exception) {
        null
    }
}

/**
 * Parses a W3C traceparent string (e.g. "00-traceId-spanId-flags") and starts a new child span
 * under the remote parent. This allows linking server-side traces to a client's trace context
 * when the client cannot send traceparent as an HTTP header (e.g. EventSource/SSE).
 *
 * Returns the new [Span], or null if parsing fails.
 * Caller is responsible for calling [Span.end] and closing the scope from [Span.makeCurrent].
 */
fun createSpanFromTraceparent(traceparent: String, spanName: String = "sse-connection"): Span? {
    return try {
        val parts = traceparent.split("-")
        if (parts.size != 4) return null

        val remoteSpanContext = SpanContext.createFromRemoteParent(
            parts[1],
            parts[2],
            TraceFlags.fromHex(parts[3], 0),
            TraceState.getDefault(),
        )

        if (!remoteSpanContext.isValid) return null

        val parentContext = Context.current().with(Span.wrap(remoteSpanContext))

        GlobalOpenTelemetry.getTracer("klage-notifications-api")
            .spanBuilder(spanName)
            .setParent(parentContext)
            .startSpan()
    } catch (_: Exception) {
        null
    }
}
