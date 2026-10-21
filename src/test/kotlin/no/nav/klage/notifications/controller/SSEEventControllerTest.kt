package no.nav.klage.notifications.controller

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.notifications.config.SseShutdownSignal
import no.nav.klage.notifications.kafka.AivenKafkaClientCreator
import no.nav.klage.notifications.service.NotificationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import reactor.kafka.receiver.KafkaReceiver
import java.time.Duration
import java.util.concurrent.TimeUnit

class SSEEventControllerTest {
    private val navIdent = "Z999999"
    private val sseShutdownSignal = SseShutdownSignal()

    // Kafka receivers that never emit, so the stream stays open like in production.
    private val aivenKafkaClientCreator =
        mockk<AivenKafkaClientCreator> {
            every { getNewKafkaNotificationInternalEventsReceiver() } returns neverEmittingReceiver()
            every { getNewKafkaNotificationInternalChangeEventsReceiver() } returns neverEmittingReceiver()
            every { getNewKafkaNotificationInternalSystemEventsReceiver() } returns neverEmittingReceiver()
        }

    private val controller =
        SSEEventController(
            aivenKafkaClientCreator = aivenKafkaClientCreator,
            notificationService =
                mockk<NotificationService> {
                    every { getNotificationsByNavIdent(navIdent) } returns emptyList()
                    every { getAllSystemNotifications() } returns emptyList()
                },
            tokenUtil = mockk { every { getIdent() } returns navIdent },
            environment = mockk(),
            sseShutdownSignal = sseShutdownSignal,
        )

    @Test
    fun `completes open stream when the application shuts down`() {
        val events =
            controller
                .notificationEvents(traceparent = null)
                .doOnSubscribe {
                    Schedulers.parallel().schedule({ sseShutdownSignal.onShutdown() }, 200, TimeUnit.MILLISECONDS)
                }.collectList()
                .block(Duration.ofSeconds(5))!!

        assertThat(events.map(ServerSentEvent<Any>::event)).containsExactly("HEARTBEAT")
    }

    @Test
    fun `completes immediately when connecting after shutdown has started`() {
        sseShutdownSignal.onShutdown()

        val events =
            controller
                .notificationEvents(traceparent = null)
                .collectList()
                .block(Duration.ofSeconds(5))!!

        assertThat(events).isEmpty()
    }

    private fun <V : Any> neverEmittingReceiver(): KafkaReceiver<String, V> = mockk { every { receive() } returns Flux.never() }
}
