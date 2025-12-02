package no.nav.klage.notifications.service

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import no.nav.klage.notifications.domain.*
import no.nav.klage.notifications.repository.NotificationRepository
import no.nav.klage.notifications.util.getLogger
import org.springframework.stereotype.Service

@Service
class NotificationAggregateMetricsService(
    private val notificationRepository: NotificationRepository,
    private val meterRegistry: MeterRegistry
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private const val METRIC_PREFIX = "klage_notifications"
        
        // Aggregate metric names
        private const val NOTIFICATIONS_PER_BEHANDLING_MIN = "${METRIC_PREFIX}_per_behandling_min_gauge"
        private const val NOTIFICATIONS_PER_BEHANDLING_MAX = "${METRIC_PREFIX}_per_behandling_max_gauge"
        private const val NOTIFICATIONS_PER_BEHANDLING_AVG = "${METRIC_PREFIX}_per_behandling_avg_gauge"
        
        private const val NOTIFICATIONS_PER_USER_MIN = "${METRIC_PREFIX}_per_user_min_gauge"
        private const val NOTIFICATIONS_PER_USER_MAX = "${METRIC_PREFIX}_per_user_max_gauge"
        private const val NOTIFICATIONS_PER_USER_AVG = "${METRIC_PREFIX}_per_user_avg_gauge"

        private const val UNREAD_NOTIFICATIONS_COUNT = "${METRIC_PREFIX}_unread_total_gauge"

        private const val TYPE_TAG = "notification_type"
    }

    private val behandlingMinByType = mutableMapOf<NotificationType, Double>()
    private val behandlingMaxByType = mutableMapOf<NotificationType, Double>()
    private val behandlingAvgByType = mutableMapOf<NotificationType, Double>()

    private val userMinByType = mutableMapOf<NotificationType, Double>()
    private val userMaxByType = mutableMapOf<NotificationType, Double>()
    private val userAvgByType = mutableMapOf<NotificationType, Double>()

    private val unreadCountByType = mutableMapOf<NotificationType, Double>()

    @PostConstruct
    fun initializeGauges() {
        // Initialize maps with default values and register gauges for each notification type
        NotificationType.entries.forEach { type ->
            // Initialize default values
            behandlingMinByType[type] = 0.0
            behandlingMaxByType[type] = 0.0
            behandlingAvgByType[type] = 0.0
            userMinByType[type] = 0.0
            userMaxByType[type] = 0.0
            userAvgByType[type] = 0.0
            unreadCountByType[type] = 0.0

            // Register behandling gauges
            Gauge.builder(NOTIFICATIONS_PER_BEHANDLING_MIN, this) { behandlingMinByType[type] ?: 0.0 }
                .tag(TYPE_TAG, type.name)
                .description("Minimum number of ${type.name} notifications per behandling")
                .register(meterRegistry)

            Gauge.builder(NOTIFICATIONS_PER_BEHANDLING_MAX, this) { behandlingMaxByType[type] ?: 0.0 }
                .tag(TYPE_TAG, type.name)
                .description("Maximum number of ${type.name} notifications per behandling")
                .register(meterRegistry)

            Gauge.builder(NOTIFICATIONS_PER_BEHANDLING_AVG, this) { behandlingAvgByType[type] ?: 0.0 }
                .tag(TYPE_TAG, type.name)
                .description("Average number of ${type.name} notifications per behandling")
                .register(meterRegistry)

            // Register user gauges
            Gauge.builder(NOTIFICATIONS_PER_USER_MIN, this) { userMinByType[type] ?: 0.0 }
                .tag(TYPE_TAG, type.name)
                .description("Minimum number of ${type.name} notifications per user (navIdent)")
                .register(meterRegistry)

            Gauge.builder(NOTIFICATIONS_PER_USER_MAX, this) { userMaxByType[type] ?: 0.0 }
                .tag(TYPE_TAG, type.name)
                .description("Maximum number of ${type.name} notifications per user (navIdent)")
                .register(meterRegistry)

            Gauge.builder(NOTIFICATIONS_PER_USER_AVG, this) { userAvgByType[type] ?: 0.0 }
                .tag(TYPE_TAG, type.name)
                .description("Average number of ${type.name} notifications per user (navIdent)")
                .register(meterRegistry)

            // Register unread count gauge
            Gauge.builder(UNREAD_NOTIFICATIONS_COUNT, this) { unreadCountByType[type] ?: 0.0 }
                .tag(TYPE_TAG, type.name)
                .description("Current number of unread ${type.name} notifications")
                .register(meterRegistry)
        }

        logger.debug("Initialized notification aggregate metric gauges with notification_type dimension")
    }

    /**
     * Calculate and update all aggregate metrics.
     * This method is called by the scheduled job.
     */
    fun updateAggregateMetrics() {
        try {
            val startTime = System.currentTimeMillis()
            logger.debug("Starting calculation of aggregate notification metrics")

            val fetchStartTime = System.currentTimeMillis()
            val allNotifications = notificationRepository.findAll()
            val fetchDuration = System.currentTimeMillis() - fetchStartTime
            logger.debug("Fetched {} notifications from repository in {} ms", allNotifications.size, fetchDuration)

            val behandlingStartTime = System.currentTimeMillis()
            updateBehandlingMetrics(allNotifications = allNotifications)
            val behandlingDuration = System.currentTimeMillis() - behandlingStartTime
            logger.debug("Updated behandling metrics in {} ms", behandlingDuration)

            val userStartTime = System.currentTimeMillis()
            updateUserMetrics(allNotifications = allNotifications)
            val userDuration = System.currentTimeMillis() - userStartTime
            logger.debug("Updated user metrics in {} ms", userDuration)

            val unreadStartTime = System.currentTimeMillis()
            updateUnreadMetrics(allNotifications = allNotifications)
            val unreadDuration = System.currentTimeMillis() - unreadStartTime
            logger.debug("Updated unread metrics in {} ms", unreadDuration)

            val totalDuration = System.currentTimeMillis() - startTime
            logger.debug("Updated aggregate metrics for all notification types in {} ms total (fetch: {} ms, behandling: {} ms, user: {} ms, unread: {} ms)",
                totalDuration, fetchDuration, behandlingDuration, userDuration, unreadDuration)
        } catch (e: Exception) {
            logger.error("Failed to update aggregate notification metrics", e)
        }
    }

    private fun updateBehandlingMetrics(allNotifications: List<Notification>) {
        try {
            // Group by type and then by behandlingId
            NotificationType.entries.forEach { type ->
                val notificationsOfType = when (type) {
                    NotificationType.MELDING -> allNotifications.filterIsInstance<MeldingNotification>()
                    NotificationType.LOST_ACCESS -> allNotifications.filterIsInstance<LostAccessNotification>()
                    NotificationType.GAINED_ACCESS -> allNotifications.filterIsInstance<GainedAccessNotification>()
                }

                // Filter only non-deleted notifications
                val activeNotifications = notificationsOfType.filter { !it.markedAsDeleted }

                // Group by behandlingId and count
                val countsByBehandling = activeNotifications
                    .groupBy {
                        when (it) {
                            is MeldingNotification -> it.behandlingId
                            is LostAccessNotification -> it.behandlingId
                            is GainedAccessNotification -> it.behandlingId
                            else -> null
                        }
                    }
                    .filter { it.key != null }
                    .values
                    .map { it.size.toLong() }

                if (countsByBehandling.isEmpty()) {
                    behandlingMinByType[type] = 0.0
                    behandlingMaxByType[type] = 0.0
                    behandlingAvgByType[type] = 0.0
                } else {
                    behandlingMinByType[type] = countsByBehandling.minOrNull()?.toDouble() ?: 0.0
                    behandlingMaxByType[type] = countsByBehandling.maxOrNull()?.toDouble() ?: 0.0
                    behandlingAvgByType[type] = countsByBehandling.average()

                    logger.debug(
                        "{} behandling metrics: min={}, max={}, avg={} from {} behandlinger",
                        type.name,
                        behandlingMinByType[type],
                        behandlingMaxByType[type],
                        behandlingAvgByType[type],
                        countsByBehandling.size
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to update behandling metrics", e)
        }
    }

    private fun updateUserMetrics(allNotifications: List<Notification>) {
        try {
            // Group by type and then by navIdent
            NotificationType.entries.forEach { type ->
                val notificationsOfType = when (type) {
                    NotificationType.MELDING -> allNotifications.filterIsInstance<MeldingNotification>()
                    NotificationType.LOST_ACCESS -> allNotifications.filterIsInstance<LostAccessNotification>()
                    NotificationType.GAINED_ACCESS -> allNotifications.filterIsInstance<GainedAccessNotification>()
                }

                // Filter only non-deleted notifications
                val activeNotifications = notificationsOfType.filter { !it.markedAsDeleted }

                // Group by navIdent and count
                val countsByUser = activeNotifications
                    .groupBy { it.navIdent }
                    .values
                    .map { it.size.toLong() }

                if (countsByUser.isEmpty()) {
                    userMinByType[type] = 0.0
                    userMaxByType[type] = 0.0
                    userAvgByType[type] = 0.0
                } else {
                    userMinByType[type] = countsByUser.minOrNull()?.toDouble() ?: 0.0
                    userMaxByType[type] = countsByUser.maxOrNull()?.toDouble() ?: 0.0
                    userAvgByType[type] = countsByUser.average()

                    logger.debug(
                        "{} user metrics: min={}, max={}, avg={} from {} users",
                        type.name,
                        userMinByType[type],
                        userMaxByType[type],
                        userAvgByType[type],
                        countsByUser.size
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to update user metrics", e)
        }
    }

    private fun updateUnreadMetrics(allNotifications: List<Notification>) {
        try {
            // Count unread notifications by type
            NotificationType.entries.forEach { type ->
                val notificationsOfType = when (type) {
                    NotificationType.MELDING -> allNotifications.filterIsInstance<MeldingNotification>()
                    NotificationType.LOST_ACCESS -> allNotifications.filterIsInstance<LostAccessNotification>()
                    NotificationType.GAINED_ACCESS -> allNotifications.filterIsInstance<GainedAccessNotification>()
                }

                // Count unread and non-deleted notifications
                val unreadCount = notificationsOfType
                    .count { !it.read && !it.markedAsDeleted }
                    .toDouble()

                unreadCountByType[type] = unreadCount

                logger.debug(
                    "{} unread count: {}",
                    type.name,
                    unreadCount
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to update unread metrics", e)
        }
    }
}