package no.nav.klage.notifications.util

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import org.apache.kafka.common.header.Headers

fun currentTraceparent(): String? {
    return try {
        val spanContext = Span.current().spanContext
        "00-${spanContext.traceId}-${spanContext.spanId}-${spanContext.traceFlags.asHex()}"
    } catch (_: Exception) {
        null
    }
}

/**
 * Parses a W3C traceparent string (e.g. "00-traceId-spanId-flags") into a [SpanContext].
 * Returns null if the string is malformed or the resulting context is invalid.
 */
fun parseTraceparent(traceparent: String): SpanContext? {
    return try {
        val parts = traceparent.split("-")
        if (parts.size != 4) return null

        val spanContext = SpanContext.createFromRemoteParent(
            parts[1],
            parts[2],
            TraceFlags.fromHex(parts[3], 0),
            TraceState.getDefault(),
        )

        if (!spanContext.isValid) null else spanContext
    } catch (_: Exception) {
        null
    }
}

/**
 * Parses a W3C traceparent string and starts a new child span under the remote parent.
 * Optionally adds a [Span Link](https://opentelemetry.io/docs/concepts/signals/traces/#span-links)
 * to another trace via [linkTraceparent].
 *
 * Returns the new [Span], or null if parsing fails.
 * Caller is responsible for calling [Span.end] and closing the scope from [Span.makeCurrent].
 */
fun createSpanFromTraceparent(
    traceparent: String,
    spanName: String = "sse-connection",
    linkTraceparent: String? = null,
): Span? {
    return try {
        val remoteSpanContext = parseTraceparent(traceparent) ?: return null
        val parentContext = Context.current().with(Span.wrap(remoteSpanContext))

        val spanBuilder = GlobalOpenTelemetry.getTracer("klage-notifications-api")
            .spanBuilder(spanName)
            .setParent(parentContext)

        if (linkTraceparent != null) {
            parseTraceparent(linkTraceparent)?.let { spanBuilder.addLink(it) }
        }

        spanBuilder.startSpan()
    } catch (_: Exception) {
        null
    }
}

/**
 * Extracts the W3C traceparent string from Kafka record headers.
 * The OTEL agent injects this header when producing messages via instrumented KafkaTemplate/KafkaProducer.
 *
 * Reactor-kafka consumers do not automatically propagate this trace context through the reactive pipeline,
 * so we extract it manually to carry it alongside the event data.
 */
fun extractTraceparentFromKafkaHeaders(headers: Headers): String? {
    return try {
        headers.lastHeader("traceparent")?.value()?.let { String(it) }
    } catch (_: Exception) {
        null
    }
}

/**
 * Runs [block] within a span created from the given [traceparent] (parent).
 * Optionally links to [linkTraceparent] (e.g. the SSE client's trace).
 * This makes [currentTraceparent] return a child of the given trace during the block's execution.
 * If [traceparent] is null or invalid, the block runs without a custom span.
 */
fun <T> withTraceparent(
    traceparent: String?,
    linkTraceparent: String? = null,
    spanName: String = "process-kafka-event",
    block: () -> T,
): T {
    if (traceparent == null) return block()
    val span = createSpanFromTraceparent(traceparent, spanName, linkTraceparent) ?: return block()
    val scope = span.makeCurrent()
    return try {
        block()
    } finally {
        scope.close()
        span.end()
    }
}
