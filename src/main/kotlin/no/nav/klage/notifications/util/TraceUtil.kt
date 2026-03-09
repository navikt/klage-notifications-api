package no.nav.klage.notifications.util

import io.opentelemetry.api.trace.Span

fun currentTraceparent(): String? {
    return try {
        val spanContext = Span.current().spanContext
        "00-${spanContext.traceId}-${spanContext.spanId}-${spanContext.traceFlags.asHex()}"
    } catch (_: Exception) {
        null
    }
}