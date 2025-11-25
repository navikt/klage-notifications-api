package no.nav.klage.notifications.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import no.nav.klage.notifications.domain.*
import no.nav.klage.notifications.util.getLogger
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class NotificationMetricsService(
    private val meterRegistry: MeterRegistry
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val METRIC_PREFIX = "klage_notifications"

        // Metric names
        private const val CREATED_METRIC = "${METRIC_PREFIX}_created_total"
        private const val READ_METRIC = "${METRIC_PREFIX}_read_total"
        private const val UNREAD_METRIC = "${METRIC_PREFIX}_unread_total"
        private const val DELETED_METRIC = "${METRIC_PREFIX}_deleted_total"
        private const val TIME_TO_READ_METRIC = "${METRIC_PREFIX}_time_to_read_seconds"

        // Tag keys
        private const val TYPE_TAG = "notification_type"
        private const val SOURCE_TAG = "source"
    }

    fun recordNotificationCreated(notification: Notification) {
        try {
            val type = getNotificationType(notification)
            val source = notification.source.name
            val behandlingId = getBehandlingId(notification)

            Counter.builder(CREATED_METRIC)
                .tag(TYPE_TAG, type)
                .tag(SOURCE_TAG, source)
                .tag("navIdent", notification.navIdent)
                .tag("behandlingId", behandlingId)
                .description("Total number of notifications created")
                .register(meterRegistry)
                .increment()
        } catch (e: Exception) {
            logger.error("Failed to record notification created metric for notification ${notification.id}", e)
        }
    }

    fun recordSystemNotificationCreated(notification: SystemNotification) {
        try {
            val source = notification.source.name

            Counter.builder(CREATED_METRIC)
                .tag(TYPE_TAG, "SYSTEM")
                .tag(SOURCE_TAG, source)
                .tag("navIdent", "ALL") // System notifications are for all users
                .tag("behandlingId", "NONE") // System notifications are not tied to a behandling
                .description("Total number of notifications created")
                .register(meterRegistry)
                .increment()
        } catch (e: Exception) {
            logger.error("Failed to record system notification created metric for notification ${notification.id}", e)
        }
    }

    fun recordNotificationRead(notification: Notification) {
        try {
            val type = getNotificationType(notification)
            val source = notification.source.name
            val behandlingId = getBehandlingId(notification)

            Counter.builder(READ_METRIC)
                .tag(TYPE_TAG, type)
                .tag(SOURCE_TAG, source)
                .tag("navIdent", notification.navIdent)
                .tag("behandlingId", behandlingId)
                .description("Total number of notifications marked as read")
                .register(meterRegistry)
                .increment()

            // Record time to read if we have both createdAt and readAt
            notification.readAt?.let { readAt ->
                val timeToRead = Duration.between(notification.sourceCreatedAt, readAt)

                Timer.builder(TIME_TO_READ_METRIC)
                    .tag(TYPE_TAG, type)
                    .tag(SOURCE_TAG, source)
                    .tag("navIdent", notification.navIdent)
                    .tag("behandlingId", behandlingId)
                    .description("Time taken for notifications to be read")
                    .register(meterRegistry)
                    .record(timeToRead)
            }
        } catch (e: Exception) {
            logger.error("Failed to record notification read metric for notification ${notification.id}", e)
        }
    }

    fun recordSystemNotificationRead(notification: SystemNotification, readAt: LocalDateTime, navIdent: String) {
        try {
            val source = notification.source.name

            Counter.builder(READ_METRIC)
                .tag(TYPE_TAG, "SYSTEM")
                .tag(SOURCE_TAG, source)
                .tag("navIdent", navIdent) // Track which user read the system notification
                .tag("behandlingId", "NONE")
                .description("Total number of notifications marked as read")
                .register(meterRegistry)
                .increment()

            // Record time to read
            val timeToRead = Duration.between(notification.createdAt, readAt)

            Timer.builder(TIME_TO_READ_METRIC)
                .tag(TYPE_TAG, "SYSTEM")
                .tag(SOURCE_TAG, source)
                .tag("navIdent", navIdent)
                .tag("behandlingId", "NONE")
                .description("Time taken for notifications to be read")
                .register(meterRegistry)
                .record(timeToRead)
        } catch (e: Exception) {
            logger.error("Failed to record system notification read metric for notification ${notification.id}", e)
        }
    }

    fun recordNotificationUnread(notification: Notification) {
        try {
            val type = getNotificationType(notification)
            val source = notification.source.name
            val behandlingId = getBehandlingId(notification)

            Counter.builder(UNREAD_METRIC)
                .tag(TYPE_TAG, type)
                .tag(SOURCE_TAG, source)
                .tag("navIdent", notification.navIdent)
                .tag("behandlingId", behandlingId)
                .description("Total number of notifications marked as unread")
                .register(meterRegistry)
                .increment()
        } catch (e: Exception) {
            logger.error("Failed to record notification unread metric for notification ${notification.id}", e)
        }
    }

    fun recordSystemNotificationUnread(notification: SystemNotification, navIdent: String) {
        try {
            val source = notification.source.name

            Counter.builder(UNREAD_METRIC)
                .tag(TYPE_TAG, "SYSTEM")
                .tag(SOURCE_TAG, source)
                .tag("navIdent", navIdent) // Track which user marked it as unread
                .tag("behandlingId", "NONE")
                .description("Total number of notifications marked as unread")
                .register(meterRegistry)
                .increment()
        } catch (e: Exception) {
            logger.error("Failed to record system notification unread metric for notification ${notification.id}", e)
        }
    }

    fun recordNotificationDeleted(notification: Notification) {
        try {
            val type = getNotificationType(notification)
            val source = notification.source.name
            val behandlingId = getBehandlingId(notification)

            Counter.builder(DELETED_METRIC)
                .tag(TYPE_TAG, type)
                .tag(SOURCE_TAG, source)
                .tag("navIdent", notification.navIdent)
                .tag("behandlingId", behandlingId)
                .description("Total number of notifications deleted")
                .register(meterRegistry)
                .increment()
        } catch (e: Exception) {
            logger.error("Failed to record notification deleted metric for notification ${notification.id}", e)
        }
    }

    fun recordSystemNotificationDeleted(notification: SystemNotification) {
        try {
            val source = notification.source.name

            Counter.builder(DELETED_METRIC)
                .tag(TYPE_TAG, "SYSTEM")
                .tag(SOURCE_TAG, source)
                .tag("navIdent", "ALL") // System notifications affect all users
                .tag("behandlingId", "NONE")
                .description("Total number of notifications deleted")
                .register(meterRegistry)
                .increment()
        } catch (e: Exception) {
            logger.error("Failed to record system notification deleted metric for notification ${notification.id}", e)
        }
    }

    fun recordMultipleNotificationsRead(notifications: List<Notification>) {
        try {
            notifications.forEach { recordNotificationRead(it) }
        } catch (e: Exception) {
            logger.error("Failed to record multiple notifications read metrics", e)
        }
    }

    fun recordMultipleSystemNotificationsRead(notifications: List<SystemNotification>, readAt: LocalDateTime, navIdent: String) {
        try {
            notifications.forEach { recordSystemNotificationRead(it, readAt, navIdent) }
        } catch (e: Exception) {
            logger.error("Failed to record multiple system notifications read metrics", e)
        }
    }

    fun recordMultipleNotificationsUnread(notifications: List<Notification>) {
        try {
            notifications.forEach { recordNotificationUnread(it) }
        } catch (e: Exception) {
            logger.error("Failed to record multiple notifications unread metrics", e)
        }
    }

    fun recordMultipleSystemNotificationsUnread(notifications: List<SystemNotification>, navIdent: String) {
        try {
            notifications.forEach { recordSystemNotificationUnread(it, navIdent) }
        } catch (e: Exception) {
            logger.error("Failed to record multiple system notifications unread metrics", e)
        }
    }

    fun recordMultipleNotificationsDeleted(notifications: List<Notification>) {
        try {
            notifications.forEach { recordNotificationDeleted(it) }
        } catch (e: Exception) {
            logger.error("Failed to record multiple notifications deleted metrics", e)
        }
    }

    fun recordMultipleSystemNotificationsDeleted(notifications: List<SystemNotification>) {
        try {
            notifications.forEach { recordSystemNotificationDeleted(it) }
        } catch (e: Exception) {
            logger.error("Failed to record multiple system notifications deleted metrics", e)
        }
    }

    private fun getNotificationType(notification: Notification): String {
        return when (notification) {
            is MeldingNotification -> NotificationType.MELDING.name
            is LostAccessNotification -> NotificationType.LOST_ACCESS.name
            else -> error("Unknown notification type")
        }
    }

    private fun getBehandlingId(notification: Notification): String {
        return when (notification) {
            is MeldingNotification -> notification.behandlingId.toString()
            is LostAccessNotification -> notification.behandlingId.toString()
            else -> error("Unknown notification type")
        }
    }
}