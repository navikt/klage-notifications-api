package no.nav.klage.notifications.controller

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.notifications.config.SseShutdownSignal
import no.nav.klage.notifications.domain.SystemNotification
import no.nav.klage.notifications.dto.NotificationChangeEvent
import no.nav.klage.notifications.kafka.AivenKafkaClientCreator
import no.nav.klage.notifications.service.NotificationService
import org.apache.kafka.common.header.internals.RecordHeaders
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverRecord
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
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

    @Test
    fun `skips invalid change events and keeps the stream open`() {
        val validId = UUID.randomUUID()
        val changeEvents =
            listOf(
                // Deserialization failure: ErrorHandlingDeserializer gives a null value
                receiverRecord<NotificationChangeEvent>(value = null, offset = 0),
                // DELETED without id, as published by deleteNotificationsByBehandlingId before the fix
                receiverRecord(value = changeEvent(type = NotificationChangeEvent.Type.DELETED, id = null), offset = 1),
                receiverRecord(value = changeEvent(type = NotificationChangeEvent.Type.READ, id = validId), offset = 2),
            )
        val kafkaClientCreator =
            mockk<AivenKafkaClientCreator> {
                every { getNewKafkaNotificationInternalEventsReceiver() } returns neverEmittingReceiver()
                every { getNewKafkaNotificationInternalChangeEventsReceiver() } returns emittingReceiver(changeEvents)
                every { getNewKafkaNotificationInternalSystemEventsReceiver() } returns neverEmittingReceiver()
            }

        val events = collectUntilShutdown(controller(kafkaClientCreator))

        assertThat(events.map(ServerSentEvent<Any>::event)).containsExactly("HEARTBEAT", "read")
    }

    @Test
    fun `skips system notification that fails to map and keeps the stream open`() {
        val failing = systemNotification()
        val working = systemNotification()
        val kafkaClientCreator =
            mockk<AivenKafkaClientCreator> {
                every { getNewKafkaNotificationInternalEventsReceiver() } returns neverEmittingReceiver()
                every { getNewKafkaNotificationInternalChangeEventsReceiver() } returns neverEmittingReceiver()
                every { getNewKafkaNotificationInternalSystemEventsReceiver() } returns
                    emittingReceiver(listOf(receiverRecord(value = failing, offset = 0), receiverRecord(value = working, offset = 1)))
            }
        val notificationService =
            mockk<NotificationService> {
                every { getNotificationsByNavIdent(navIdent) } returns emptyList()
                every { getAllSystemNotifications() } returns emptyList()
                every { isSystemNotificationReadByUser(failing.id, navIdent) } throws RuntimeException("database unavailable")
                every { isSystemNotificationReadByUser(working.id, navIdent) } returns false
            }

        val events = collectUntilShutdown(controller(kafkaClientCreator, notificationService))

        assertThat(events.map(ServerSentEvent<Any>::event)).containsExactly("HEARTBEAT", "create")
        assertThat(events.last().id()).endsWith(working.id.toString())
    }

    private fun controller(
        kafkaClientCreator: AivenKafkaClientCreator,
        notificationService: NotificationService =
            mockk {
                every { getNotificationsByNavIdent(navIdent) } returns emptyList()
                every { getAllSystemNotifications() } returns emptyList()
            },
    ) = SSEEventController(
        aivenKafkaClientCreator = kafkaClientCreator,
        notificationService = notificationService,
        tokenUtil = mockk { every { getIdent() } returns navIdent },
        environment = mockk { every { activeProfiles } returns arrayOf("test") },
        sseShutdownSignal = sseShutdownSignal,
    )

    private fun collectUntilShutdown(controller: SSEEventController): List<ServerSentEvent<Any>> =
        controller
            .notificationEvents(traceparent = null)
            .doOnSubscribe {
                Schedulers.parallel().schedule({ sseShutdownSignal.onShutdown() }, 500, TimeUnit.MILLISECONDS)
            }.collectList()
            .block(Duration.ofSeconds(5))!!

    private fun changeEvent(
        type: NotificationChangeEvent.Type,
        id: UUID?,
    ) = NotificationChangeEvent(id = id, ids = null, navIdent = navIdent, type = type, updatedAt = LocalDateTime.now())

    private fun systemNotification() =
        SystemNotification(title = "title", message = "message", createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

    private fun <V : Any> receiverRecord(
        value: V?,
        offset: Long,
    ): ReceiverRecord<String, V> =
        mockk(relaxed = true) {
            every { value() } returns value
            every { offset() } returns offset
            every { headers() } returns RecordHeaders()
        }

    private fun <V : Any> emittingReceiver(records: List<ReceiverRecord<String, V>>): KafkaReceiver<String, V> =
        mockk { every { receive() } returns Flux.fromIterable(records).concatWith(Flux.never()) }

    private fun <V : Any> neverEmittingReceiver(): KafkaReceiver<String, V> = mockk { every { receive() } returns Flux.never() }
}
