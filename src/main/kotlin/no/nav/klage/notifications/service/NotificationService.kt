package no.nav.klage.notifications.service

import com.fasterxml.jackson.databind.JsonNode
import no.nav.klage.notifications.domain.LostAccessNotification
import no.nav.klage.notifications.domain.MeldingNotification
import no.nav.klage.notifications.domain.Notification
import no.nav.klage.notifications.domain.NotificationType
import no.nav.klage.notifications.dto.CreateLostAccessNotificationRequest
import no.nav.klage.notifications.dto.CreateMeldingNotificationEvent
import no.nav.klage.notifications.dto.NotificationChangeEvent
import no.nav.klage.notifications.exceptions.MissingAccessException
import no.nav.klage.notifications.exceptions.NotificationNotFoundException
import no.nav.klage.notifications.repository.LostAccessNotificationRepository
import no.nav.klage.notifications.repository.MeldingNotificationRepository
import no.nav.klage.notifications.repository.NotificationRepository
import no.nav.klage.notifications.util.getLogger
import no.nav.klage.notifications.util.ourJacksonObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val meldingNotificationRepository: MeldingNotificationRepository,
    private val lostAccessNotificationRepository: LostAccessNotificationRepository,
    private val kafkaInternalEventService: KafkaInternalEventService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val objectMapper = ourJacksonObjectMapper()
    }

    @Transactional(readOnly = true)
    fun getNotificationsByNavIdent(navIdent: String): List<Notification> {
        return notificationRepository.findByNavIdentAndMarkedAsDeletedOrderBySourceCreatedAtAsc(
            navIdent = navIdent,
            markedAsDeleted = false,
        )
    }

    fun markAsRead(id: UUID, navIdent: String) {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }

        if (notification.navIdent != navIdent) {
            throw MissingAccessException("User with navIdent $navIdent does not have access to notification with id $id")
        }

        notification.read = true
        notification.readAt = LocalDateTime.now()
        notification.updatedAt = LocalDateTime.now()

        val notificationChangeEvent = NotificationChangeEvent(
            id = notification.id,
            navIdent = notification.navIdent,
            type = NotificationChangeEvent.Type.READ,
            updatedAt = notification.updatedAt,
        )

        kafkaInternalEventService.publishInternalNotificationChangeEvent(
            jsonNode = objectMapper.valueToTree(notificationChangeEvent)
        )
    }

    fun markMultipleAsRead(notificationIdList: List<UUID>, navIdent: String) {
        val notifications = notificationRepository.findAllById(notificationIdList)

        notifications.forEach { notification ->
            if (notification.navIdent != navIdent) {
                throw MissingAccessException("User with navIdent $navIdent does not have access to notification with id ${notification.id}")
            }
            notification.read = true
            notification.readAt = LocalDateTime.now()
            notification.updatedAt = LocalDateTime.now()

            val notificationChangeEvent = NotificationChangeEvent(
                id = notification.id,
                navIdent = notification.navIdent,
                type = NotificationChangeEvent.Type.READ,
                updatedAt = notification.updatedAt,
            )

            kafkaInternalEventService.publishInternalNotificationChangeEvent(
                jsonNode = objectMapper.valueToTree(notificationChangeEvent)
            )
        }
    }

    fun setUnread(id: UUID, navIdent: String) {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }

        if (notification.navIdent != navIdent) {
            throw MissingAccessException("User with navIdent $navIdent does not have access to notification with id $id")
        }

        notification.read = false
        notification.readAt = null
        notification.updatedAt = LocalDateTime.now()

        val notificationChangeEvent = NotificationChangeEvent(
            id = notification.id,
            navIdent = notification.navIdent,
            type = NotificationChangeEvent.Type.UNREAD,
            updatedAt = notification.updatedAt,
        )

        kafkaInternalEventService.publishInternalNotificationChangeEvent(
            jsonNode = objectMapper.valueToTree(notificationChangeEvent)
        )
    }

    fun markAllAsReadForUser(navIdent: String) {
        val notifications =
            notificationRepository.findByNavIdentAndRead(navIdent, false)

        notifications.forEach { notification ->
            notification.read = true
            notification.readAt = LocalDateTime.now()
            notification.updatedAt = LocalDateTime.now()
        }

        notifications.forEach { notification ->
            val notificationChangeEvent = NotificationChangeEvent(
                id = notification.id,
                navIdent = notification.navIdent,
                type = NotificationChangeEvent.Type.READ,
                updatedAt = notification.updatedAt,
            )

            kafkaInternalEventService.publishInternalNotificationChangeEvent(
                jsonNode = objectMapper.valueToTree(notificationChangeEvent)
            )
        }
    }

    fun deleteNotification(id: UUID) {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }

        notification.markedAsDeleted = true
        notification.updatedAt = LocalDateTime.now()

        val notificationChangeEvent = NotificationChangeEvent(
            id = notification.id,
            navIdent = notification.navIdent,
            type = NotificationChangeEvent.Type.DELETED,
            updatedAt = notification.updatedAt,
        )

        kafkaInternalEventService.publishInternalNotificationChangeEvent(
            jsonNode = objectMapper.valueToTree(notificationChangeEvent)
        )
    }

    fun processNotificationMessage(messageId: UUID, jsonNode: JsonNode) {
        try {
            val type = NotificationType.valueOf(jsonNode.get("type").asText())
            logger.debug("Processing notification message with id {} of type {}", messageId, type)

            val notification = when (type) {
                NotificationType.MELDING -> {
                    val request = objectMapper.treeToValue(
                        jsonNode,
                        CreateMeldingNotificationEvent::class.java,
                    )
                    createMeldingNotification(
                        request = request,
                        kafkaMessageId = messageId,
                    )
                }

                NotificationType.LOST_ACCESS -> {
                    val request = objectMapper.treeToValue(
                        jsonNode,
                        CreateLostAccessNotificationRequest::class.java,
                    )
                    createLostAccessNotification(
                        request = request,
                        kafkaMessageId = messageId,
                    )
                }
            }

            val jsonNodeToPassOn: JsonNode = when (notification) {
                is MeldingNotification -> {
                    objectMapper.valueToTree(
                        notification
                    )
                }

                is LostAccessNotification -> {
                    objectMapper.valueToTree(
                        notification
                    )
                }

                else -> { error("Unsupported notification type: ${notification::class.simpleName}") }
            }

            kafkaInternalEventService.publishInternalNotificationEvent(
                jsonNode = jsonNodeToPassOn,
            )

            logger.debug("Successfully processed notification message with id {}", messageId)

        } catch (e: Exception) {
            logger.error("Error processing notification message with id $messageId: ${e.message}", e)
            throw e
        }
    }

    fun createMeldingNotification(request: CreateMeldingNotificationEvent, kafkaMessageId: UUID): MeldingNotification {
        val now = LocalDateTime.now()
        val notification = MeldingNotification(
            message = request.message,
            navIdent = request.recipientNavIdent,
            read = false,
            source = request.source,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = kafkaMessageId,
            sourceCreatedAt = request.sourceCreatedAt,
            behandlingId = request.behandlingId,
            meldingId = request.meldingId,
            actorNavIdent = request.actorNavIdent,
            actorNavn = request.actorNavn,
            saksnummer = request.saksnummer,
            ytelse = request.ytelse,
            behandlingType = request.behandlingType,
        )

        return meldingNotificationRepository.save(notification)
    }

    fun createLostAccessNotification(request: CreateLostAccessNotificationRequest, kafkaMessageId: UUID): LostAccessNotification {
        val now = LocalDateTime.now()
        val notification = LostAccessNotification(
            message = request.message,
            navIdent = request.recipientNavIdent,
            read = false,
            source = request.source,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = kafkaMessageId,
            sourceCreatedAt = request.sourceCreatedAt,
            behandlingId = request.behandlingId,
            saksnummer = request.saksnummer,
            ytelse = request.ytelse,
            behandlingType = request.behandlingType,
        )

        return lostAccessNotificationRepository.save(notification)
    }
}