package no.nav.klage.notifications.service

import no.nav.klage.notifications.domain.LostAccessNotification
import no.nav.klage.notifications.domain.MeldingNotification
import no.nav.klage.notifications.domain.Notification
import no.nav.klage.notifications.dto.CreateLostAccessNotificationRequest
import no.nav.klage.notifications.dto.CreateMeldingNotificationEvent
import no.nav.klage.notifications.dto.CreateNotificationEvent
import no.nav.klage.notifications.dto.NotificationChangeEvent
import no.nav.klage.notifications.exceptions.MissingAccessException
import no.nav.klage.notifications.exceptions.NotificationNotFoundException
import no.nav.klage.notifications.repository.LostAccessNotificationRepository
import no.nav.klage.notifications.repository.MeldingNotificationRepository
import no.nav.klage.notifications.repository.NotificationRepository
import no.nav.klage.notifications.util.getLogger
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
    }

    @Transactional(readOnly = true)
    fun getNotificationsByNavIdent(navIdent: String): List<Notification> {
        logger.debug("Fetching notifications for navIdent {}", navIdent)
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
            notificationChangeEvent = notificationChangeEvent
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
                notificationChangeEvent = notificationChangeEvent
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
            notificationChangeEvent = notificationChangeEvent
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
                notificationChangeEvent = notificationChangeEvent
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
            notificationChangeEvent = notificationChangeEvent
        )
    }

    fun deleteNotificationsByBehandlingId(behandlingId: UUID) {
        logger.debug("Deleting all notifications for behandlingId {}", behandlingId)

        val notifications = notificationRepository.findAllByBehandlingId(behandlingId)

        if (notifications.isEmpty()) {
            logger.warn("No notifications found for behandlingId {}", behandlingId)
            return
        }

        notifications.forEach { notification ->
            notification.markedAsDeleted = true
            notification.updatedAt = LocalDateTime.now()

            val notificationChangeEvent = NotificationChangeEvent(
                id = notification.id,
                navIdent = notification.navIdent,
                type = NotificationChangeEvent.Type.DELETED,
                updatedAt = notification.updatedAt,
            )

            kafkaInternalEventService.publishInternalNotificationChangeEvent(
                notificationChangeEvent = notificationChangeEvent
            )
        }

        logger.debug("Marked {} notifications as deleted for behandlingId {}", notifications.size, behandlingId)
    }

    fun processNotificationMessage(kafkaMessageId: UUID, createNotificationEvent: CreateNotificationEvent) {
        try {
            logger.debug("Processing notification message with id {} of type {}", kafkaMessageId, createNotificationEvent.type)

            val notification = when (createNotificationEvent) {
                is CreateMeldingNotificationEvent -> {
                    createMeldingNotification(
                        event = createNotificationEvent,
                        kafkaMessageId = kafkaMessageId,
                    )
                }
                is CreateLostAccessNotificationRequest -> {
                    createLostAccessNotification(
                        request = createNotificationEvent,
                        kafkaMessageId = kafkaMessageId,
                    )
                }
            }

            kafkaInternalEventService.publishInternalNotificationEvent(
                notification = notification,
            )

            logger.debug("Successfully processed notification message with kafkaMessageId {}", kafkaMessageId)

        } catch (e: Exception) {
            logger.error("Error processing notification message with kafkaMessageId $kafkaMessageId: ${e.message}", e)
            throw e
        }
    }

    fun createMeldingNotification(event: CreateMeldingNotificationEvent, kafkaMessageId: UUID): MeldingNotification {
        val now = LocalDateTime.now()
        val notification = MeldingNotification(
            message = event.message,
            navIdent = event.recipientNavIdent,
            read = false,
            source = event.source,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = kafkaMessageId,
            sourceCreatedAt = event.sourceCreatedAt,
            behandlingId = event.behandlingId,
            meldingId = event.meldingId,
            actorNavIdent = event.actorNavIdent,
            actorNavn = event.actorNavn,
            saksnummer = event.saksnummer,
            ytelse = event.ytelse,
            behandlingType = event.behandlingType,
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