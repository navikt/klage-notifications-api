package no.nav.klage.notifications.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.notifications.config.SecurityConfiguration
import no.nav.klage.notifications.domain.MeldingNotification
import no.nav.klage.notifications.domain.Notification
import no.nav.klage.notifications.dto.NotificationChangeEvent
import no.nav.klage.notifications.dto.view.*
import no.nav.klage.notifications.dto.view.NotificationType.MESSAGE
import no.nav.klage.notifications.kafka.AivenKafkaClientCreator
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.TokenUtil
import no.nav.klage.notifications.util.getLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration


@RestController
@Tag(name = "user", description = "API for user notifications")
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
class SSEEventController(
    private val aivenKafkaClientCreator: AivenKafkaClientCreator,
    private val notificationService: NotificationService,
    private val tokenUtil: TokenUtil,
    private val environment: Environment,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Operation(
        summary = "Subscribe to real-time notification events",
        description = """
Server-Sent Events (SSE) endpoint that streams notification events in real-time.

**Event Types:**
- `create` - New notification created or existing notifications loaded
- `read` - Notification marked as read
- `unread` - Notification marked as unread
- `delete` - Notification deleted
- `HEARTBEAT` - Keep-alive heartbeat (sent every 10 seconds)

**Notification Types in 'create' events:**
- MESSAGE - Message notifications with actor, behandling info, and content
- LOST_ACCESS - Access lost notifications (to be implemented)
"""
    )
    @ApiResponse(
        responseCode = "200",
        description = "SSE stream of notification events",
        content = [Content(
            mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
            schema = Schema(implementation = ServerSentEvent::class),
            examples = [
                ExampleObject(
                    name = "create_message_notification",
                    summary = "Create event - MESSAGE notification",
                    description = "Event fired when a new message notification is created or loaded",
                    value = """
event: create
id: 2025-11-16T10:30:00_550e8400-e29b-41d4-a716-446655440000
data: {
  "type": "MESSAGE",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "read": false,
  "createdAt": "2025-11-16T10:30:00",
  "message": {
    "id": "750e8400-e29b-41d4-a716-446655440000",
    "content": "New message about the case"
  },
  "actor": {
    "navIdent": "A123456",
    "navn": "Ola Nordmann"
  },
  "behandling": {
    "id": "650e8400-e29b-41d4-a716-446655440000",
    "typeId": "1",
    "ytelseId": "10",
    "saksnummer": "2025-12345"
  }
}
"""
                ),
                ExampleObject(
                    name = "read_notification",
                    summary = "Read event",
                    description = "Event fired when a notification is marked as read",
                    value = """
event: read
id: 2025-11-16T10:35:00_550e8400-e29b-41d4-a716-446655440000
data: {
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
"""
                ),
                ExampleObject(
                    name = "unread_notification",
                    summary = "Unread event",
                    description = "Event fired when a notification is marked as unread",
                    value = """
event: unread
id: 2025-11-16T10:36:00_550e8400-e29b-41d4-a716-446655440000
data: {
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
"""
                ),
                ExampleObject(
                    name = "delete_notification",
                    summary = "Delete event",
                    description = "Event fired when a notification is deleted",
                    value = """
event: delete
id: 2025-11-16T10:37:00_550e8400-e29b-41d4-a716-446655440000
data: {
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
"""
                ),
                ExampleObject(
                    name = "heartbeat",
                    summary = "Heartbeat event",
                    description = "Keep-alive heartbeat sent every 10 seconds",
                    value = """
                        event: HEARTBEAT
                    """
                )
            ]
        )]
    )
    @GetMapping("/user/notifications/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun notificationEvents(): Flux<ServerSentEvent<Any>> {

        logger.debug("New SSE connection for notification events established by navIdent=${tokenUtil.getIdent()}")

        //https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-async-disconnects
        val heartbeatStream = getHeartbeatStream()

        val navIdent = tokenUtil.getIdent()

        val internalNotificationEventPublisher = getInternalNotificationEventPublisher(
            navIdent = navIdent,
        )

        val internalNotificationChangeEventPublisher = getInternalNotificationChangeEventPublisher(
            navIdent = navIdent,
        )

        val previousNotificationsAsSSEEvents =
            getPreviousNotificationsAsSSEEvents(notificationService.getNotificationsByNavIdent(navIdent = navIdent))

        return getFirstHeartbeat()
            .mergeWith(heartbeatStream)
            .mergeWith(previousNotificationsAsSSEEvents)
            .mergeWith(internalNotificationChangeEventPublisher)
            .mergeWith(internalNotificationEventPublisher)
    }

    private fun getPreviousNotificationsAsSSEEvents(notificationsByNavIdent: List<Notification>): Flux<ServerSentEvent<Any>> {
        return Flux.fromIterable(
            notificationsByNavIdent
                .map {
                    val data = dbToInternalNotificationEvent(notification = it)
                    ServerSentEvent.builder<Any>()
                        .id("${it.updatedAt}_${it.id}")
                        .event(Action.CREATE.lower)
                        .data(data)
                        .build()
                }
        )
    }

    private fun getInternalNotificationEventPublisher(navIdent: String): Flux<ServerSentEvent<Any>> {
        return sharedInternalEvents
            .filter { (recipientNavIdent, _) -> recipientNavIdent == navIdent }
            .map { (_, data) ->
                val id = when (data) {
                    is MessageNotification -> "${data.createdAt}_${data.id}"
                    else -> TODO()
                }
                ServerSentEvent.builder<Any>()
                    .id(id)
                    .event(Action.CREATE.lower)
                    .data(data)
                    .build()
            }
    }

    private fun getInternalNotificationChangeEventPublisher(navIdent: String): Flux<ServerSentEvent<Any>> {
        return sharedChangeEvents
            .filter { (recipientNavIdent, _) -> recipientNavIdent == navIdent }
            .map { (_, changeEvent) ->
                ServerSentEvent.builder<Any>()
                    .id("${changeEvent.updatedAt}_${changeEvent.id}")
                    .event(
                        when (changeEvent.type) {
                            NotificationChangeEvent.Type.READ -> Action.READ.lower
                            NotificationChangeEvent.Type.UNREAD -> Action.UNREAD.lower
                            NotificationChangeEvent.Type.DELETED -> Action.DELETE.lower
                        }
                    )
                    .data(NotificationChanged(id = changeEvent.id))
                    .build()
            }
    }

    private fun getFirstHeartbeat(): Flux<ServerSentEvent<Any>> {
        val emitFirstHeartbeat = Flux.generate {
            it.next(toHeartBeatServerSentEvent())
            it.complete()
        }
        return emitFirstHeartbeat
    }

    private fun getHeartbeatStream(
    ): Flux<ServerSentEvent<Any>> {
        val heartbeatStream: Flux<ServerSentEvent<Any>> = Flux.interval(Duration.ofSeconds(10))
            .map {
                toHeartBeatServerSentEvent()
            }
        return heartbeatStream
    }

    private fun toHeartBeatServerSentEvent(): ServerSentEvent<Any> {
        return ServerSentEvent.builder<Any>()
            .event("HEARTBEAT")
            .build()
    }

    private fun dbToInternalNotificationEvent(notification: Notification): Any {
        return when (notification) {
            is MeldingNotification -> {
                MessageNotification(
                    type = MESSAGE,
                    id = notification.id,
                    read = notification.read,
                    createdAt = notification.sourceCreatedAt,
                    message = MessageNotification.Message(
                        id = notification.meldingId,
                        content = notification.message,
                    ),
                    actor = NavEmployee(
                        navIdent = notification.actorNavIdent,
                        navn = notification.actorNavn,
                    ),
                    behandling = BehandlingInfo(
                        id = notification.behandlingId,
                        typeId = notification.behandlingType.id,
                        ytelseId = notification.ytelse.id,
                        saksnummer = notification.saksnummer,
                    )
                )
            }

            else -> {
                TODO()
            }
        }
    }

    private fun jsonToInternalNotificationEvent(notification: Notification): Any {
        return if (notification is MeldingNotification) {
            MessageNotification(
                type = MESSAGE,
                id = notification.id,
                read = false,
                createdAt = notification.sourceCreatedAt,
                message = MessageNotification.Message(
                    id = notification.meldingId,
                    content = notification.message,
                ),
                actor = NavEmployee(
                    navIdent = notification.actorNavIdent,
                    navn = notification.actorNavn,
                ),
                behandling = BehandlingInfo(
                    id = notification.behandlingId,
                    typeId = notification.behandlingType.id,
                    ytelseId = notification.ytelse.id,
                    saksnummer = notification.saksnummer,
                ),
            )
        } else TODO()
    }

    // Shared Kafka consumers - created once and shared by all clients
    private val sharedInternalEvents: Flux<Pair<String, Any>> by lazy {
        aivenKafkaClientCreator.getNewKafkaNotificationInternalEventsReceiver().receive()
            .doOnNext { consumerRecord ->
                logger.debug("Received internal notification event at offset {}: {}", consumerRecord.offset(), consumerRecord.key())
                if (environment.activeProfiles.contains("dev-gcp")) {
                    logger.debug(
                        "Received internal Kafka-message (notification): {}",
                        consumerRecord.value()
                    )
                }
            }
            .mapNotNull { consumerRecord ->
                try {
                    val data = jsonToInternalNotificationEvent(consumerRecord.value())
                    // Acknowledge after successful processing
                    consumerRecord.receiverOffset().acknowledge()
                    Pair(consumerRecord.value().navIdent, data)
                } catch (e: Exception) {
                    logger.error("Error processing internal notification event at offset {}: ${e.message}", consumerRecord.offset(), e)
                    null // Don't acknowledge - message will be reprocessed
                }
            }
            .share() // Share among all subscribers
    }

    private val sharedChangeEvents: Flux<Pair<String, NotificationChangeEvent>> by lazy {
        aivenKafkaClientCreator.getNewKafkaNotificationInternalChangeEventsReceiver().receive()
            .doOnNext { consumerRecord ->
                if (environment.activeProfiles.contains("dev-gcp")) {
                    logger.debug(
                        "Received internal Kafka-message (notification change) at offset {}: {}",
                        consumerRecord.offset(),
                        consumerRecord.value()
                    )
                }
            }
            .mapNotNull { consumerRecord ->
                try {
                    val changeEvent = consumerRecord.value()
                    // Acknowledge after successful processing
                    consumerRecord.receiverOffset().acknowledge()
                    Pair(changeEvent.navIdent, changeEvent)
                } catch (e: Exception) {
                    logger.error("Error processing internal change event at offset {}: ${e.message}", consumerRecord.offset(), e)
                    null // Don't acknowledge - message will be reprocessed
                }
            }
            .share() // Share among all subscribers
    }
}