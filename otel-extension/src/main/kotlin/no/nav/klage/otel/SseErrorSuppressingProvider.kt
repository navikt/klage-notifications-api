package no.nav.klage.otel

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import io.opentelemetry.sdk.trace.export.SpanExporter

/**
 * OTEL Java agent extension that suppresses spurious ERROR statuses on the SSE
 * notification endpoint span(s) caused by clients disconnecting.
 *
 * Why an agent extension and not a servlet filter?
 *   With `Flux<ServerSentEvent>` on the servlet stack, client-disconnect
 *   IOExceptions are captured by Reactor/Spring's async machinery and reported
 *   to the OTEL agent via `AsyncListener.onError`. They never propagate back
 *   through the servlet filter chain, so an `OncePerRequestFilter` cannot
 *   intercept them. The only reliable point of interception is the
 *   `SpanExporter` — every span that the agent produces flows through it
 *   before being exported.
 *
 * Implementation: we wrap the configured `SpanExporter` and rewrite the status
 * of matching spans from ERROR to OK before they are exported. Registered via
 * the `AutoConfigurationCustomizerProvider` SPI (see
 * `META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider`).
 *
 * NOTE on logging: we deliberately use `System.out.println` instead of any
 * logging framework. The extension runs inside the OTEL agent's classloader,
 * loaded extremely early in JVM startup — before Logback, before Spring Boot's
 * `jul-to-slf4j` bridge, and before the agent itself has finalized its own
 * logging config. JUL output from here often disappears in containerized
 * deploys. stdout is always captured by Kubernetes, so we print there with a
 * clearly-greppable prefix and accept the small ugliness for reliable
 * diagnostics. Volume is tiny (a handful at startup + one per actual SSE
 * disconnect).
 */
class SseErrorSuppressingProvider : AutoConfigurationCustomizerProvider {

    init {
        println("$LOG_PREFIX SseErrorSuppressingProvider instance constructed")
    }

    override fun customize(autoConfiguration: AutoConfigurationCustomizer) {
        println("$LOG_PREFIX customize() called — registering SpanExporter wrapper")
        autoConfiguration.addSpanExporterCustomizer { exporter, _ ->
            println("$LOG_PREFIX wrapping exporter ${exporter.javaClass.name}")
            SseErrorSuppressingSpanExporter(exporter)
        }
    }

    companion object {
        // Module-wide log prefix so all messages from this extension are easy to
        // grep for in stdout (e.g. `kubectl logs ... | grep '\[otel-sse-ext\]'`).
        internal const val LOG_PREFIX = "[otel-sse-ext]"

        init {
            // Runs the first time the class is loaded by the JVM. If we don't
            // see this line in the pod logs, the extension JAR is not on the
            // agent's extension path at all.
            println("$LOG_PREFIX SseErrorSuppressingProvider class loaded")
        }
    }
}

internal class SseErrorSuppressingSpanExporter(
    private val delegate: SpanExporter,
) : SpanExporter {

    override fun export(spans: Collection<SpanData>): CompletableResultCode {
        val rewritten = spans.map { sd ->
            if (shouldRewrite(sd)) {
                println(
                    "${SseErrorSuppressingProvider.LOG_PREFIX} rewriting ERROR -> OK on SSE span " +
                        "name='${sd.name}' traceId=${sd.traceId} spanId=${sd.spanId} " +
                        "events=${sd.events.size} statusDesc='${sd.status.description}'"
                )
                StatusOverridingSpanData(sd, OK_STATUS)
            } else {
                sd
            }
        }
        return delegate.export(rewritten)
    }

    override fun flush(): CompletableResultCode = delegate.flush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()

    private fun shouldRewrite(sd: SpanData): Boolean {
        if (sd.status.statusCode != StatusCode.ERROR) return false
        if (!isSseSpan(sd)) return false
        return looksLikeClientDisconnect(sd)
    }

    private fun isSseSpan(sd: SpanData): Boolean {
        val attrs = sd.attributes
        val route = attrs.get(HTTP_ROUTE)
        if (route != null && route.contains(SSE_PATH)) return true

        val urlPath = attrs.get(URL_PATH)
        if (urlPath != null && urlPath.contains(SSE_PATH)) return true

        val httpTarget = attrs.get(HTTP_TARGET)
        if (httpTarget != null && httpTarget.contains(SSE_PATH)) return true

        // Fallback: the agent may use the route as the span name (e.g. "GET /user/notifications/events").
        return sd.name.contains(SSE_PATH)
    }

    private fun looksLikeClientDisconnect(sd: SpanData): Boolean {
        for (event in sd.events) {
            val message = event.attributes.get(EXCEPTION_MESSAGE)
            val type = event.attributes.get(EXCEPTION_TYPE)
            if (DISCONNECT_INDICATORS.any { needle ->
                    message?.contains(needle, ignoreCase = true) == true ||
                        type?.contains(needle, ignoreCase = true) == true
                }
            ) {
                return true
            }
        }

        val description = sd.status.description
        if (description != null && DISCONNECT_INDICATORS.any { description.contains(it, ignoreCase = true) }) {
            return true
        }

        // Last-resort fallback: on the SSE endpoint, any ERROR with no captured
        // exception is overwhelmingly likely to be a client disconnect that the
        // agent recorded only as a non-2xx response status. Rewrite it too.
        if (sd.events.isEmpty()) {
            return true
        }

        return false
    }

    private companion object {
        private val HTTP_ROUTE = AttributeKey.stringKey("http.route")
        private val URL_PATH = AttributeKey.stringKey("url.path")
        private val HTTP_TARGET = AttributeKey.stringKey("http.target")
        private val EXCEPTION_MESSAGE = AttributeKey.stringKey("exception.message")
        private val EXCEPTION_TYPE = AttributeKey.stringKey("exception.type")

        // Path of the SSE endpoint defined in SSEEventController.
        private const val SSE_PATH = "/user/notifications/events"

        // Substrings of exception messages / types that indicate a client disconnect.
        private val DISCONNECT_INDICATORS = listOf(
            "Broken pipe",
            "Connection reset by peer",
            "ClientAbortException",
            "AsyncRequestNotUsableException",
            "EofException",
            "Response already committed",
        )

        private val OK_STATUS: StatusData = StatusData.create(
            StatusCode.OK,
            "Client disconnected (expected for SSE)",
        )
    }
}

/**
 * Delegating `SpanData` that overrides only the status. All other accessors
 * return the values from the wrapped instance.
 */
internal class StatusOverridingSpanData(
    private val delegate: SpanData,
    private val overriddenStatus: StatusData,
) : SpanData by delegate {
    override fun getStatus(): StatusData = overriddenStatus
}
