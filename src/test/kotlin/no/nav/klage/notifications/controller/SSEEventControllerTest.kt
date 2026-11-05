package no.nav.klage.notifications.controller

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.notifications.config.SseShutdownSignal
import no.nav.klage.notifications.domain.LostAccessNotification
import no.nav.klage.notifications.domain.SystemNotification
import no.nav.klage.notifications.dto.InternalNotificationEvent
import no.nav.klage.notifications.dto.NotificationChangeEvent
import no.nav.klage.notifications.kafka.InternalKafkaEventStreams
import no.nav.klage.notifications.service.NotificationService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class SSEEventControllerTest {
    private val navIdent = "Z999999"
    private val sseShutdownSignal = SseShutdownSignal()

    // Event streams that never emit, so the stream stays open like in production.
    private val defaultEventStreams = eventStreams()

    private val controller =
        SSEEventController(
            internalKafkaEventStreams = defaultEventStreams,
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
                consumerRecord<NotificationChangeEvent>(value = null, offset = 0),
                // DELETED without id, as published by deleteNotificationsByBehandlingId before the fix
                consumerRecord(value = changeEvent(type = NotificationChangeEvent.Type.DELETED, id = null), offset = 1),
                consumerRecord(value = changeEvent(type = NotificationChangeEvent.Type.READ, id = validId), offset = 2),
            )
        val eventStreams = eventStreams(changeEvents = emittingStream(changeEvents))

        val events = collectUntilShutdown(controller(eventStreams = eventStreams))

        assertThat(events.map(ServerSentEvent<Any>::event)).containsExactly("HEARTBEAT", "read")
    }

    @Test
    fun `skips system notification that fails to map and keeps the stream open`() {
        val failing = systemNotification()
        val working = systemNotification()
        val eventStreams =
            eventStreams(
                systemEvents =
                    emittingStream(
                        listOf(consumerRecord(value = failing, offset = 0), consumerRecord(value = working, offset = 1)),
                    ),
            )
        val notificationService =
            mockk<NotificationService> {
                every { getNotificationsByNavIdent(navIdent) } returns emptyList()
                every { getAllSystemNotifications() } returns emptyList()
                every { isSystemNotificationReadByUser(systemNotificationId = failing.id, navIdent = navIdent) } throws
                    RuntimeException("database unavailable")
                every { isSystemNotificationReadByUser(systemNotificationId = working.id, navIdent = navIdent) } returns false
            }

        val events = collectUntilShutdown(controller(eventStreams = eventStreams, notificationService = notificationService))

        assertThat(events.map(ServerSentEvent<Any>::event)).containsExactly("HEARTBEAT", "create")
        assertThat(events.last().id()).endsWith(working.id.toString())
    }

    @Test
    fun `only delivers notification events addressed to the connected navIdent`() {
        val otherNotificationId = UUID.randomUUID()
        val matchingNotificationId = UUID.randomUUID()
        val notificationEvents =
            listOf(
                consumerRecord(
                    value =
                        InternalNotificationEvent(
                            notifications = listOf(lostAccessNotification(notificationId = otherNotificationId, navIdent = "Z111111")),
                        ),
                    offset = 0,
                ),
                consumerRecord(
                    value =
                        InternalNotificationEvent(
                            notifications = listOf(lostAccessNotification(notificationId = matchingNotificationId, navIdent = navIdent)),
                        ),
                    offset = 1,
                ),
            )
        val eventStreams = eventStreams(notificationEvents = emittingStream(notificationEvents))

        val events = collectUntilShutdown(controller(eventStreams = eventStreams))

        assertThat(events.map(ServerSentEvent<Any>::event)).containsExactly("HEARTBEAT", "create")
        assertThat(events.last().id()).endsWith(matchingNotificationId.toString())
    }

    private fun controller(
        eventStreams: InternalKafkaEventStreams,
        notificationService: NotificationService =
            mockk {
                every { getNotificationsByNavIdent(navIdent) } returns emptyList()
                every { getAllSystemNotifications() } returns emptyList()
            },
    ) = SSEEventController(
        internalKafkaEventStreams = eventStreams,
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

    private fun eventStreams(
        notificationEvents: Flux<ConsumerRecord<String, InternalNotificationEvent?>> = neverEmittingStream(),
        changeEvents: Flux<ConsumerRecord<String, NotificationChangeEvent?>> = neverEmittingStream(),
        systemEvents: Flux<ConsumerRecord<String, SystemNotification?>> = neverEmittingStream(),
    ): InternalKafkaEventStreams =
        mockk {
            every { internalNotificationEvents() } returns notificationEvents
            every { internalChangeEvents() } returns changeEvents
            every { internalSystemEvents() } returns systemEvents
        }

    private fun changeEvent(
        type: NotificationChangeEvent.Type,
        id: UUID?,
    ) = NotificationChangeEvent(id = id, ids = null, navIdent = navIdent, type = type, updatedAt = LocalDateTime.now())

    private fun lostAccessNotification(
        notificationId: UUID,
        navIdent: String,
    ) = LostAccessNotification(
        id = notificationId,
        message = "message",
        navIdent = navIdent,
        read = false,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        readAt = null,
        markedAsDeleted = false,
        kafkaMessageId = null,
        sourceCreatedAt = LocalDateTime.now(),
        behandlingId = UUID.randomUUID(),
        saksnummer = "saksnummer",
        ytelse = mockk { every { id } returns "1" },
        behandlingType = mockk { every { id } returns "1" },
    )

    private fun systemNotification() =
        SystemNotification(title = "title", message = "message", createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())

    private fun <V : Any> consumerRecord(
        value: V?,
        offset: Long,
    ): ConsumerRecord<String, V?> = ConsumerRecord("topic", 0, offset, "key", value)

    private fun <V : Any> emittingStream(records: List<ConsumerRecord<String, V?>>): Flux<ConsumerRecord<String, V?>> =
        Flux.fromIterable(records).concatWith(Flux.never())

    private fun <V : Any> neverEmittingStream(): Flux<ConsumerRecord<String, V?>> = Flux.never()
}
