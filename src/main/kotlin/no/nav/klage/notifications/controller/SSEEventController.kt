package no.nav.klage.notifications.controller

import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.notifications.config.SecurityConfiguration
import no.nav.klage.notifications.domain.MeldingNotification
import no.nav.klage.notifications.domain.Notification
import no.nav.klage.notifications.domain.NotificationType
import no.nav.klage.notifications.dto.view.BehandlingInfo
import no.nav.klage.notifications.dto.view.MeldingNotificationEvent
import no.nav.klage.notifications.dto.view.NavEmployee
import no.nav.klage.notifications.dto.view.NotificationType.MESSAGE
import no.nav.klage.notifications.kafka.AivenKafkaClientCreator
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.TokenUtil
import no.nav.klage.notifications.util.getLogger
import no.nav.klage.notifications.util.ourJacksonObjectMapper
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val objectMapper = ourJacksonObjectMapper()
    }

    @GetMapping("/user/notificationevents", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun events(): Flux<ServerSentEvent<JsonNode>> {
        //https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-async-disconnects
        val heartbeatStream: Flux<ServerSentEvent<JsonNode>> = getHeartbeatStream()

        val navIdent = tokenUtil.getIdent()
        val internalNotificationEventPublisher = getInternalNotificationEventPublisher(
            navIdent = navIdent,
        )

        val emitFirstHeartbeat = getFirstHeartbeat()

        return getNotificationsAsSSEEvents(notificationService.getNotificationsByNavIdent(navIdent = navIdent))
            .mergeWith(internalNotificationEventPublisher)
            .mergeWith(emitFirstHeartbeat)
            .mergeWith(heartbeatStream)
    }

    private fun getNotificationsAsSSEEvents(notificationsByNavIdent: List<Notification>): Flux<ServerSentEvent<JsonNode>> {
        return Flux.fromIterable(notificationsByNavIdent
            .map {
                val jsonNode = dbToInternalNotificationEvent(notification = it)
                ServerSentEvent.builder<JsonNode>()
                    .id("${it.sourceCreatedAt}_${it.id}")
                    .event("create")
                    .data(jsonNode)
                    .build()
            }
        )
    }

    private fun getInternalNotificationEventPublisher(
        navIdent: String,
    ): Flux<ServerSentEvent<JsonNode>> {
        val flux = aivenKafkaClientCreator.getNewKafkaNotificationInternalEventsReceiver().receive()
            .mapNotNull { consumerRecord ->
                val jsonNode = objectMapper.readTree(consumerRecord.value())
                val recipientNavIdent = jsonNode.get("navIdent").asText()
                if (recipientNavIdent == navIdent) {
                    val jsonNodeToReturnToClient = jsonToInternalNotificationEvent(jsonNode)
                    val id = jsonNodeToReturnToClient.get("id").asText()
                    val sourceCreatedAt = jsonNodeToReturnToClient.get("sourceCreatedAt").asText()
                    ServerSentEvent.builder<JsonNode>()
                        .id("${sourceCreatedAt}_$id")
                        .event("create")
                        .data(jsonNodeToReturnToClient)
                        .build()
                } else null
            }

        return flux
    }

    private fun getFirstHeartbeat(): Flux<ServerSentEvent<JsonNode>> {
        val emitFirstHeartbeat = Flux.generate<ServerSentEvent<JsonNode>> {
            it.next(toHeartBeatServerSentEvent())
            it.complete()
        }
        return emitFirstHeartbeat
    }

    private fun getHeartbeatStream(
    ): Flux<ServerSentEvent<JsonNode>> {
        val heartbeatStream: Flux<ServerSentEvent<JsonNode>> = Flux.interval(Duration.ofSeconds(10))
            .map {
                toHeartBeatServerSentEvent()
            }
        return heartbeatStream
    }

    private fun toHeartBeatServerSentEvent(): ServerSentEvent<JsonNode> {
        return ServerSentEvent.builder<JsonNode>()
            .event("HEARTBEAT")
            .build()
    }

    private fun dbToInternalNotificationEvent(notification: Notification): JsonNode {
        return when (notification) {
            is MeldingNotification -> {
                objectMapper.valueToTree(
                    MeldingNotificationEvent(
                        type = MESSAGE,
                        id = notification.meldingId,
                        read = false,
                        createdAt = notification.meldingCreated,
                        content = notification.message,
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
                )
            }

            else -> {
                TODO()
            }

        }
    }

    private fun jsonToInternalNotificationEvent(jsonNode: JsonNode): JsonNode {
        return when (NotificationType.valueOf(jsonNode.get("type").asText())) {
            NotificationType.MELDING -> {
                val request = objectMapper.treeToValue(
                    jsonNode,
                    MeldingNotification::class.java,
                )

                objectMapper.valueToTree(
                    MeldingNotificationEvent(
                        type = MESSAGE,
                        id = request.meldingId,
                        read = false,
                        createdAt = request.meldingCreated,
                        content = request.message,
                        actor = NavEmployee(
                            navIdent = request.actorNavIdent,
                            navn = request.actorNavn,
                        ),
                        behandling = BehandlingInfo(
                            id = request.behandlingId,
                            typeId = request.behandlingType.id,
                            ytelseId = request.ytelse.id,
                            saksnummer = request.saksnummer,
                        ),
                    )
                )
            }

            NotificationType.LOST_ACCESS -> {
                TODO()
            }
        }
    }
}