package no.nav.klage.notifications.service

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import no.nav.klage.notifications.domain.LostAccessNotification
import no.nav.klage.notifications.domain.MeldingNotification
import no.nav.klage.notifications.domain.Notification
import no.nav.klage.notifications.domain.NotificationType
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
        private const val NOTIFICATIONS_PER_BEHANDLING_MIN = "${METRIC_PREFIX}_per_behandling_min"
        private const val NOTIFICATIONS_PER_BEHANDLING_MAX = "${METRIC_PREFIX}_per_behandling_max"
        private const val NOTIFICATIONS_PER_BEHANDLING_AVG = "${METRIC_PREFIX}_per_behandling_avg"
        
        private const val NOTIFICATIONS_PER_USER_MIN = "${METRIC_PREFIX}_per_user_min"
        private const val NOTIFICATIONS_PER_USER_MAX = "${METRIC_PREFIX}_per_user_max"
        private const val NOTIFICATIONS_PER_USER_AVG = "${METRIC_PREFIX}_per_user_avg"

        private const val TYPE_TAG = "notification_type"
    }

    private val behandlingMinByType = mutableMapOf<NotificationType, Double>()
    private val behandlingMaxByType = mutableMapOf<NotificationType, Double>()
    private val behandlingAvgByType = mutableMapOf<NotificationType, Double>()

    private val userMinByType = mutableMapOf<NotificationType, Double>()
    private val userMaxByType = mutableMapOf<NotificationType, Double>()
    private val userAvgByType = mutableMapOf<NotificationType, Double>()

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
        }

        logger.debug("Initialized notification aggregate metric gauges with notification_type dimension")
    }

    /**
     * Calculate and update all aggregate metrics.
     * This method is called by the scheduled job.
     */
    fun updateAggregateMetrics() {
        try {
            logger.debug("Starting calculation of aggregate notification metrics")

            val allNotifications = notificationRepository.findAll()

            updateBehandlingMetrics(allNotifications = allNotifications)
            updateUserMetrics(allNotifications = allNotifications)

            logger.debug("Updated aggregate metrics for all notification types")
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
                }

                // Filter only non-deleted notifications
                val activeNotifications = notificationsOfType.filter { !it.markedAsDeleted }

                // Group by behandlingId and count
                val countsByBehandling = activeNotifications
                    .groupBy {
                        when (it) {
                            is MeldingNotification -> it.behandlingId
                            is LostAccessNotification -> it.behandlingId
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
}