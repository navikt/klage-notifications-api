package no.nav.klage.notifications.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.annotation.PostConstruct
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
        //means created. "created" was stripped by prometheus, so no point in using it in the name.
        private const val CREATED_METRIC = "${METRIC_PREFIX}_total_counter"

        private const val READ_METRIC = "${METRIC_PREFIX}_read_events_total_counter"
        private const val UNREAD_METRIC = "${METRIC_PREFIX}_unread_events_total_counter"
        private const val DELETED_METRIC = "${METRIC_PREFIX}_deleted_events_total_counter"
        private const val TIME_TO_READ_METRIC = "${METRIC_PREFIX}_time_to_read_seconds_timer"

        // Tag keys
        private const val TYPE_TAG = "notification_type"

        // Custom histogram buckets for time to read (in seconds)
        private val TIME_TO_READ_BUCKETS = doubleArrayOf(
            30.0,                  // 30 seconds
            60.0,                  // 1 minute
            180.0,                 // 3 minutes
            600.0,                 // 10 minutes
            1200.0,                // 20 minutes
            3600.0,                // 1 hour
            10800.0,               // 3 hours
            28800.0,               // 8 hours
            86400.0,               // 1 day
            259200.0,              // 3 days
            604800.0,              // 1 week
            1209600.0,             // 2 weeks
            2419200.0,             // 4 weeks
            4838400.0              // 8 weeks
            // +Inf is automatically added by Micrometer
        )
    }

    @PostConstruct
    fun initializeCounters() {
        // Initialize all counter combinations so they start at 0
        val notificationTypes = listOf(
            NotificationType.MELDING.name,
            NotificationType.LOST_ACCESS.name,
            NotificationType.GAINED_ACCESS.name,
            "SYSTEM"
        )

        notificationTypes.forEach { type ->
            // Initialize created counter
            Counter.builder(CREATED_METRIC)
                .tag(TYPE_TAG, type)
                .description("Total number of notifications created")
                .register(meterRegistry)

            // Initialize read counter
            Counter.builder(READ_METRIC)
                .tag(TYPE_TAG, type)
                .description("Total number of notifications marked as read")
                .register(meterRegistry)

            // Initialize unread counter
            Counter.builder(UNREAD_METRIC)
                .tag(TYPE_TAG, type)
                .description("Total number of notifications marked as unread")
                .register(meterRegistry)

            // Initialize deleted counter
            Counter.builder(DELETED_METRIC)
                .tag(TYPE_TAG, type)
                .description("Total number of notifications deleted")
                .register(meterRegistry)

            // Initialize time to read timer
            Timer.builder(TIME_TO_READ_METRIC)
                .tag(TYPE_TAG, type)
                .description("Time taken for notifications to be read")
                .serviceLevelObjectives(*TIME_TO_READ_BUCKETS.map { Duration.ofSeconds(it.toLong()) }.toTypedArray())
                .register(meterRegistry)
        }

        logger.debug("Initialized all notification counters and timers")
    }

    fun recordNotificationCreated(notification: Notification) {
        try {
            val type = getNotificationType(notification)
            Counter.builder(CREATED_METRIC)
                .tag(TYPE_TAG, type)
                .description("Total number of notifications created")
                .register(meterRegistry)
                .increment()
        } catch (e: Exception) {
            logger.error("Failed to record notification created metric for notification ${notification.id}", e)
        }
    }

    fun recordSystemNotificationCreated(notification: SystemNotification) {
        try {
            Counter.builder(CREATED_METRIC)
                .tag(TYPE_TAG, "SYSTEM")
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
            Counter.builder(READ_METRIC)
                .tag(TYPE_TAG, type)
                .description("Total number of notifications marked as read")
                .register(meterRegistry)
                .increment()
            // Record time to read if we have both createdAt and readAt
            notification.readAt?.let { readAt ->
                val timeToRead = Duration.between(notification.sourceCreatedAt, readAt)
                Timer.builder(TIME_TO_READ_METRIC)
                    .tag(TYPE_TAG, type)
                    .description("Time taken for notifications to be read")
                    .serviceLevelObjectives(*TIME_TO_READ_BUCKETS.map { Duration.ofSeconds(it.toLong()) }.toTypedArray())
                    .register(meterRegistry)
                    .record(timeToRead)
            }
        } catch (e: Exception) {
            logger.error("Failed to record notification read metric for notification ${notification.id}", e)
        }
    }

    fun recordSystemNotificationRead(notification: SystemNotification, readAt: LocalDateTime) {
        try {
            Counter.builder(READ_METRIC)
                .tag(TYPE_TAG, "SYSTEM")
                .description("Total number of notifications marked as read")
                .register(meterRegistry)
                .increment()
            // Record time to read
            val timeToRead = Duration.between(notification.createdAt, readAt)
            Timer.builder(TIME_TO_READ_METRIC)
                .tag(TYPE_TAG, "SYSTEM")
                .description("Time taken for notifications to be read")
                .serviceLevelObjectives(*TIME_TO_READ_BUCKETS.map { Duration.ofSeconds(it.toLong()) }.toTypedArray())
                .register(meterRegistry)
                .record(timeToRead)
        } catch (e: Exception) {
            logger.error("Failed to record system notification read metric for notification ${notification.id}", e)
        }
    }

    fun recordNotificationUnread(notification: Notification) {
        try {
            val type = getNotificationType(notification)
            Counter.builder(UNREAD_METRIC)
                .tag(TYPE_TAG, type)
                .description("Total number of notifications marked as unread")
                .register(meterRegistry)
                .increment()
        } catch (e: Exception) {
            logger.error("Failed to record notification unread metric for notification ${notification.id}", e)
        }
    }

    fun recordSystemNotificationUnread(notification: SystemNotification) {
        try {
            Counter.builder(UNREAD_METRIC)
                .tag(TYPE_TAG, "SYSTEM")
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
            Counter.builder(DELETED_METRIC)
                .tag(TYPE_TAG, type)
                .description("Total number of notifications deleted")
                .register(meterRegistry)
                .increment()
        } catch (e: Exception) {
            logger.error("Failed to record notification deleted metric for notification ${notification.id}", e)
        }
    }

    fun recordSystemNotificationDeleted(notification: SystemNotification) {
        try {
            Counter.builder(DELETED_METRIC)
                .tag(TYPE_TAG, "SYSTEM")
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

    fun recordMultipleSystemNotificationsRead(notifications: List<SystemNotification>, readAt: LocalDateTime) {
        try {
            notifications.forEach { recordSystemNotificationRead(it, readAt) }
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

    fun recordMultipleSystemNotificationsUnread(notifications: List<SystemNotification>) {
        try {
            notifications.forEach { recordSystemNotificationUnread(it) }
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
            is GainedAccessNotification -> NotificationType.GAINED_ACCESS.name
            else -> error("Unknown notification type")
        }
    }
}
