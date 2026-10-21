package no.nav.klage.notifications.config

import no.nav.klage.notifications.util.getLogger
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

/**
 * Lets SSE streams complete cleanly when the application shuts down.
 *
 * ContextClosedEvent is published before the web server's graceful shutdown starts waiting for
 * active requests, so open streams end instead of holding up shutdown until the timeout.
 * Sinks.one keeps the emitted value, so streams opened after shutdown has started also complete immediately.
 */
@Component
class SseShutdownSignal {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    private val sink = Sinks.one<Unit>()

    fun asMono(): Mono<Unit> = sink.asMono()

    @EventListener(ContextClosedEvent::class)
    fun onShutdown() {
        logger.info("Application is shutting down. Completing open SSE streams.")
        sink.tryEmitValue(Unit)
    }
}
