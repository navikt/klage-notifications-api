package no.nav.klage.notifications.service

import no.nav.klage.notifications.domain.*
import no.nav.klage.notifications.dto.*
import no.nav.klage.notifications.exceptions.MissingAccessException
import no.nav.klage.notifications.exceptions.NotificationNotFoundException
import no.nav.klage.notifications.exceptions.UnreadNotificationsException
import no.nav.klage.notifications.repository.*
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
    private val systemNotificationRepository: SystemNotificationRepository,
    private val systemNotificationReadStatusRepository: SystemNotificationReadStatusRepository,
    private val kafkaInternalEventService: KafkaInternalEventService,
    private val metricsService: NotificationMetricsService,
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
        // Try to find as regular notification first
        val regularNotification = notificationRepository.findById(id)

        if (regularNotification.isPresent) {
            val notification = regularNotification.get()

            if (notification.navIdent != navIdent) {
                throw MissingAccessException("User with navIdent $navIdent does not have access to notification with id $id")
            }

            notification.read = true
            notification.readAt = LocalDateTime.now()
            notification.updatedAt = LocalDateTime.now()

            // Record metrics
            metricsService.recordNotificationRead(notification)

            val notificationChangeEvent = NotificationChangeEvent(
                id = notification.id,
                ids = null,
                navIdent = notification.navIdent,
                type = NotificationChangeEvent.Type.READ,
                updatedAt = notification.updatedAt,
            )

            kafkaInternalEventService.publishInternalNotificationChangeEvent(
                notificationChangeEvent = notificationChangeEvent
            )
        } else {
            // Try as system notification
            markSystemNotificationAsRead(id, navIdent)
        }
    }

    fun markMultipleAsRead(notificationIdList: List<UUID>, navIdent: String) {
        if (notificationIdList.isEmpty()) return

        val now = LocalDateTime.now()

        val regularNotifications = notificationRepository.findByIdInAndNavIdent(notificationIdList, navIdent)
        val regularNotificationIds = regularNotifications.map { it.id }.toSet()

        regularNotifications.forEach { notification ->
            notification.read = true
            notification.readAt = now
            notification.updatedAt = now
        }

        // Record metrics for regular notifications
        metricsService.recordMultipleNotificationsRead(regularNotifications)

        // Find IDs that weren't regular notifications - they might be system notifications
        val remainingIds = notificationIdList.filterNot { it in regularNotificationIds }

        val systemNotifications = systemNotificationRepository.findByIdIn(remainingIds)

        // Check which ones are not already marked as read
        val existingReadStatuses = if (systemNotifications.isNotEmpty()) {
            systemNotificationReadStatusRepository.findBySystemNotificationIdInAndNavIdent(
                systemNotifications.map { it.id },
                navIdent
            )
        } else {
            emptyList()
        }

        val alreadyReadSystemNotificationIds = existingReadStatuses.map { it.systemNotificationId }.toSet()

        // Create read statuses for system notifications that aren't already read
        val systemNotificationReadStatuses = systemNotifications
            .filter { it.id !in alreadyReadSystemNotificationIds }
            .map { systemNotification ->
                SystemNotificationReadStatus(
                    systemNotificationId = systemNotification.id,
                    navIdent = navIdent,
                    readAt = now,
                )
            }

        // Batch insert new read statuses
        if (systemNotificationReadStatuses.isNotEmpty()) {
            systemNotificationReadStatusRepository.saveAll(systemNotificationReadStatuses)
            logger.debug("Marked {} system notifications as read for user {}", systemNotificationReadStatuses.size, navIdent)

            // Record metrics for system notifications
            metricsService.recordMultipleSystemNotificationsRead(systemNotifications.filter { it.id !in alreadyReadSystemNotificationIds }, now)
        }

        // Validate that all requested IDs were found
        val allFoundIds = regularNotificationIds + systemNotifications.map { it.id }.toSet()
        val notFoundIds = notificationIdList.filterNot { it in allFoundIds }
        if (notFoundIds.isNotEmpty()) {
            throw NotificationNotFoundException("Notifications not found: $notFoundIds")
        }

        // Publish a single READ_MULTIPLE event for all successfully updated notifications
        val allUpdatedIds = regularNotifications.map { it.id } + systemNotificationReadStatuses.map { it.systemNotificationId }
        if (allUpdatedIds.isNotEmpty()) {
            val notificationChangeEvent = NotificationChangeEvent(
                ids = allUpdatedIds,
                id = null,
                navIdent = navIdent,
                type = NotificationChangeEvent.Type.READ_MULTIPLE,
                updatedAt = now,
            )
            kafkaInternalEventService.publishInternalNotificationChangeEvent(
                notificationChangeEvent = notificationChangeEvent
            )
        }

        logger.debug(
            "Marked {} regular and {} system notifications as read for user {}",
            regularNotifications.size,
            systemNotificationReadStatuses.size,
            navIdent
        )
    }

    fun setUnread(id: UUID, navIdent: String) {
        // Try to find as regular notification first
        val regularNotification = notificationRepository.findById(id)

        if (regularNotification.isPresent) {
            val notification = regularNotification.get()

            if (notification.navIdent != navIdent) {
                throw MissingAccessException("User with navIdent $navIdent does not have access to notification with id $id")
            }

            notification.read = false
            notification.readAt = null
            notification.updatedAt = LocalDateTime.now()

            // Record metrics
            metricsService.recordNotificationUnread(notification)

            val notificationChangeEvent = NotificationChangeEvent(
                id = notification.id,
                ids = null,
                navIdent = notification.navIdent,
                type = NotificationChangeEvent.Type.UNREAD,
                updatedAt = notification.updatedAt,
            )

            kafkaInternalEventService.publishInternalNotificationChangeEvent(
                notificationChangeEvent = notificationChangeEvent
            )
        } else {
            // Try as system notification
            markSystemNotificationAsUnread(id, navIdent)
        }
    }

    fun markMultipleAsUnread(notificationIdList: List<UUID>, navIdent: String) {
        if (notificationIdList.isEmpty()) return

        val now = LocalDateTime.now()

        // Fetch all regular notifications for this user in one query
        val regularNotifications = notificationRepository.findByIdInAndNavIdent(notificationIdList, navIdent)
        val regularNotificationIds = regularNotifications.map { it.id }.toSet()

        // Update all regular notifications at once
        regularNotifications.forEach { notification ->
            notification.read = false
            notification.readAt = null
            notification.updatedAt = now
        }

        // Record metrics for regular notifications
        metricsService.recordMultipleNotificationsUnread(regularNotifications)

        // Find IDs that weren't regular notifications - they might be system notifications
        val remainingIds = notificationIdList.filterNot { it in regularNotificationIds }

        val systemNotifications = systemNotificationRepository.findByIdIn(remainingIds)

        // Find and delete existing read statuses for system notifications
        val existingReadStatuses = if (systemNotifications.isNotEmpty()) {
            systemNotificationReadStatusRepository.findBySystemNotificationIdInAndNavIdent(
                systemNotifications.map { it.id },
                navIdent
            )
        } else {
            emptyList()
        }

        // Delete read statuses to mark them as unread
        if (existingReadStatuses.isNotEmpty()) {
            systemNotificationReadStatusRepository.deleteAll(existingReadStatuses)
            logger.debug("Marked {} system notifications as unread for user {}", existingReadStatuses.size, navIdent)

            // Record metrics for system notifications
            val unreadSystemNotifications = systemNotifications.filter { it.id in existingReadStatuses.map { rs -> rs.systemNotificationId } }
            metricsService.recordMultipleSystemNotificationsUnread(unreadSystemNotifications)
        }

        // Validate that all requested IDs were found
        val allFoundIds = regularNotificationIds + systemNotifications.map { it.id }.toSet()
        val notFoundIds = notificationIdList.filterNot { it in allFoundIds }
        if (notFoundIds.isNotEmpty()) {
            throw NotificationNotFoundException("Notifications not found: $notFoundIds")
        }

        // Publish a single UNREAD_MULTIPLE event for all successfully updated notifications
        val allUpdatedIds = regularNotifications.map { it.id } + existingReadStatuses.map { it.systemNotificationId }
        if (allUpdatedIds.isNotEmpty()) {
            val notificationChangeEvent = NotificationChangeEvent(
                ids = allUpdatedIds,
                id = null,
                navIdent = navIdent,
                type = NotificationChangeEvent.Type.UNREAD_MULTIPLE,
                updatedAt = now,
            )
            kafkaInternalEventService.publishInternalNotificationChangeEvent(
                notificationChangeEvent = notificationChangeEvent
            )
        }

        logger.debug(
            "Marked {} regular and {} system notifications as unread for user {}",
            regularNotifications.size,
            existingReadStatuses.size,
            navIdent
        )
    }

    fun markAllAsReadForUser(navIdent: String) {
        val now = LocalDateTime.now()

        // Handle regular notifications
        val notifications =
            notificationRepository.findByNavIdentAndRead(navIdent, false)

        notifications.forEach { notification ->
            notification.read = true
            notification.readAt = now
            notification.updatedAt = now
        }

        // Record metrics for regular notifications
        metricsService.recordMultipleNotificationsRead(notifications)

        // Handle system notifications
        val allSystemNotifications = systemNotificationRepository.findByMarkedAsDeletedOrderByCreatedAtDesc(false)
        val unreadSystemNotifications = allSystemNotifications.filter { systemNotification ->
            !systemNotificationReadStatusRepository.existsBySystemNotificationIdAndNavIdent(
                systemNotification.id,
                navIdent
            )
        }

        val systemNotificationReadStatuses = unreadSystemNotifications.map { systemNotification ->
            SystemNotificationReadStatus(
                systemNotificationId = systemNotification.id,
                navIdent = navIdent,
                readAt = now,
            )
        }

        if (systemNotificationReadStatuses.isNotEmpty()) {
            systemNotificationReadStatusRepository.saveAll(systemNotificationReadStatuses)

            // Record metrics for system notifications
            metricsService.recordMultipleSystemNotificationsRead(unreadSystemNotifications, now)
        }

        // Publish a single READ_MULTIPLE event for all updated notifications
        val allUpdatedIds = notifications.map { it.id } + systemNotificationReadStatuses.map { it.systemNotificationId }
        if (allUpdatedIds.isNotEmpty()) {
            val notificationChangeEvent = NotificationChangeEvent(
                ids = allUpdatedIds,
                id = null,
                navIdent = navIdent,
                type = NotificationChangeEvent.Type.READ_MULTIPLE,
                updatedAt = now,
            )
            kafkaInternalEventService.publishInternalNotificationChangeEvent(
                notificationChangeEvent = notificationChangeEvent
            )
        }

        logger.debug(
            "Marked {} regular and {} system notifications as read for user {}",
            notifications.size,
            systemNotificationReadStatuses.size,
            navIdent
        )
    }

//    fun deleteNotification(id: UUID) {
//        val notification = notificationRepository.findById(id)
//            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }
//
//        notification.markedAsDeleted = true
//        notification.updatedAt = LocalDateTime.now()
//
//        // Record metrics
//        metricsService.recordNotificationDeleted(notification)
//
//        val notificationChangeEvent = NotificationChangeEvent(
//            id = notification.id,
//            ids = null,
//            navIdent = notification.navIdent,
//            type = NotificationChangeEvent.Type.DELETED,
//            updatedAt = notification.updatedAt,
//        )
//
//        kafkaInternalEventService.publishInternalNotificationChangeEvent(
//            notificationChangeEvent = notificationChangeEvent
//        )
//    }

    fun deleteMultipleSystemNotifications(notificationIdList: List<UUID>) {
        if (notificationIdList.isEmpty()) return

        val now = LocalDateTime.now()

        // Fetch all system notifications in bulk
        val systemNotifications = systemNotificationRepository.findAllById(notificationIdList)
        val foundIds = systemNotifications.map { it.id }.toSet()

        // Validate that all requested IDs were found
        val notFoundIds = notificationIdList.filterNot { it in foundIds }
        if (notFoundIds.isNotEmpty()) {
            throw NotificationNotFoundException("System notifications not found: $notFoundIds")
        }

        // Mark all as deleted
        systemNotifications.forEach { systemNotification ->
            systemNotification.markedAsDeleted = true
            systemNotification.updatedAt = now
        }

        // Record metrics
        metricsService.recordMultipleSystemNotificationsDeleted(systemNotifications)

        // Publish a single DELETED_MULTIPLE event to all users (navIdent = "*")
        val notificationChangeEvent = NotificationChangeEvent(
            ids = systemNotifications.map { it.id },
            id = null,
            navIdent = "*",  // Broadcast to all users since system notifications are global
            type = NotificationChangeEvent.Type.DELETED_MULTIPLE,
            updatedAt = now,
        )
        kafkaInternalEventService.publishInternalNotificationChangeEvent(
            notificationChangeEvent = notificationChangeEvent
        )

        logger.debug("Marked {} system notifications as deleted", systemNotifications.size)
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
                ids = null,
                navIdent = notification.navIdent,
                type = NotificationChangeEvent.Type.DELETED,
                updatedAt = notification.updatedAt,
            )

            kafkaInternalEventService.publishInternalNotificationChangeEvent(
                notificationChangeEvent = notificationChangeEvent
            )
        }

        // Record metrics for deleted notifications
        metricsService.recordMultipleNotificationsDeleted(notifications)

        logger.debug("Marked {} notifications as deleted for behandlingId {}", notifications.size, behandlingId)
    }

    fun validateNoUnreadNotificationsForBehandling(behandlingId: UUID) {
        logger.debug("Validating no unread notifications for behandlingId {}", behandlingId)

        val unreadNotifications = notificationRepository.findByReadAndBehandlingIdAndNotMarkedAsDeleted(
            read = false,
            behandlingId = behandlingId
        )

        if (unreadNotifications.isNotEmpty()) {
            val message =
                "Du må markere alle varsler knyttet til behandlingen som lest før du kan fullføre. Uleste varsler: ${unreadNotifications.size}."
            logger.warn(message)
            throw UnreadNotificationsException(message, unreadNotifications.size)
        }

        logger.debug("No unread notifications found for behandlingId {}", behandlingId)
    }

    fun deleteOldMarkedAsDeletedNotifications(daysOld: Int): Int {
        val cutoffDate = LocalDateTime.now().minusDays(daysOld.toLong())
        logger.debug("Finding notifications marked as deleted before {}", cutoffDate)

        // Find old regular notifications
        val oldNotifications = notificationRepository.findByMarkedAsDeletedAndUpdatedAtBefore(
            markedAsDeleted = true,
            updatedAt = cutoffDate
        )

        // Find old system notifications
        val oldSystemNotifications = systemNotificationRepository.findByMarkedAsDeletedAndUpdatedAtBefore(
            markedAsDeleted = true,
            updatedAt = cutoffDate
        )

        val totalCount = oldNotifications.size + oldSystemNotifications.size

        if (totalCount == 0) {
            logger.debug("No old deleted notifications found")
            return 0
        }

        logger.debug("Permanently deleting {} old notifications marked as deleted ({} regular, {} system)",
            totalCount, oldNotifications.size, oldSystemNotifications.size)

        notificationRepository.deleteAll(oldNotifications)
        systemNotificationRepository.deleteAll(oldSystemNotifications)

        logger.debug("Successfully deleted {} old notifications", totalCount)

        return totalCount
    }

    fun processNotificationMessage(kafkaMessageId: UUID, createNotificationEvent: CreateNotificationEvent) {
        try {
            logger.debug(
                "Processing notification message with id {} of type {}",
                kafkaMessageId,
                createNotificationEvent.type
            )

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

        val saved = meldingNotificationRepository.save(notification)

        // Record metrics
        metricsService.recordNotificationCreated(saved)

        return saved
    }

    fun createLostAccessNotification(
        request: CreateLostAccessNotificationRequest,
        kafkaMessageId: UUID
    ): LostAccessNotification {
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

        val saved = lostAccessNotificationRepository.save(notification)

        // Record metrics
        metricsService.recordNotificationCreated(saved)

        return saved
    }

    // SystemNotification methods
    fun createSystemNotification(request: CreateSystemNotificationRequest): SystemNotification {
        val now = LocalDateTime.now()
        val notification = SystemNotification(
            title = request.title,
            message = request.message,
            source = request.source,
            createdAt = now,
            updatedAt = now,
            markedAsDeleted = false,
        )

        val saved = systemNotificationRepository.save(notification)
        logger.debug("Created system notification with id {}", saved.id)

        // Record metrics
        metricsService.recordSystemNotificationCreated(saved)

        // Publish to SSE via internal Kafka topic
        kafkaInternalEventService.publishSystemNotificationEvent(saved)

        return saved
    }

    @Transactional(readOnly = true)
    fun getAllSystemNotifications(): List<SystemNotification> {
        return systemNotificationRepository.findByMarkedAsDeletedOrderByCreatedAtDesc(false)
    }

    @Transactional(readOnly = true)
    fun isSystemNotificationReadByUser(systemNotificationId: UUID, navIdent: String): Boolean {
        return systemNotificationReadStatusRepository.existsBySystemNotificationIdAndNavIdent(
            systemNotificationId,
            navIdent
        )
    }

    private fun markSystemNotificationAsRead(id: UUID, navIdent: String) {
        val systemNotification = systemNotificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("System notification with id $id not found") }

        if (!systemNotificationReadStatusRepository.existsBySystemNotificationIdAndNavIdent(id, navIdent)) {
            val now = LocalDateTime.now()
            val readStatus = SystemNotificationReadStatus(
                systemNotificationId = id,
                navIdent = navIdent,
                readAt = now,
            )
            systemNotificationReadStatusRepository.save(readStatus)
            logger.debug("Marked system notification {} as read for user {}", id, navIdent)

            // Record metrics
            metricsService.recordSystemNotificationRead(systemNotification, now)

            // Publish change event for SSE clients
            val notificationChangeEvent = NotificationChangeEvent(
                id = id,
                ids = null,
                navIdent = navIdent,
                type = NotificationChangeEvent.Type.READ,
                updatedAt = now,
            )
            kafkaInternalEventService.publishInternalNotificationChangeEvent(
                notificationChangeEvent = notificationChangeEvent
            )
        } else {
            logger.debug("System notification {} is already marked as read for user {}", id, navIdent)
        }
    }

    private fun markSystemNotificationAsUnread(id: UUID, navIdent: String) {
        val systemNotification = systemNotificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("System notification with id $id not found") }

        systemNotificationReadStatusRepository.deleteBySystemNotificationIdAndNavIdent(id, navIdent)
        logger.debug("Marked system notification {} as unread for user {}", id, navIdent)

        // Record metrics
        metricsService.recordSystemNotificationUnread(systemNotification)

        // Publish change event for SSE clients
        val notificationChangeEvent = NotificationChangeEvent(
            id = id,
            ids = null,
            navIdent = navIdent,
            type = NotificationChangeEvent.Type.UNREAD,
            updatedAt = LocalDateTime.now(),
        )
        kafkaInternalEventService.publishInternalNotificationChangeEvent(
            notificationChangeEvent = notificationChangeEvent
        )
    }

    fun deleteSystemNotification(id: UUID) {
        val notification = systemNotificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("System notification with id $id not found") }

        notification.markedAsDeleted = true
        notification.updatedAt = LocalDateTime.now()
        systemNotificationRepository.save(notification)
        logger.debug("Marked system notification {} as deleted", id)

        // Record metrics
        metricsService.recordSystemNotificationDeleted(notification)

        val notificationChangeEvent = NotificationChangeEvent(
            id = id,
            ids = null,
            navIdent = "*",
            type = NotificationChangeEvent.Type.DELETED,
            updatedAt = notification.updatedAt,
        )
        kafkaInternalEventService.publishInternalNotificationChangeEvent(
            notificationChangeEvent = notificationChangeEvent
        )
    }
}