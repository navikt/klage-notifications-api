package no.nav.klage.notifications.service

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
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
    }

    // Volatile fields to hold the current values
    @Volatile
    private var behandlingMin: Double = 0.0
    @Volatile
    private var behandlingMax: Double = 0.0
    @Volatile
    private var behandlingAvg: Double = 0.0
    
    @Volatile
    private var userMin: Double = 0.0
    @Volatile
    private var userMax: Double = 0.0
    @Volatile
    private var userAvg: Double = 0.0

    @PostConstruct
    fun initializeGauges() {
        // Register gauges that read from the volatile fields
        Gauge.builder(NOTIFICATIONS_PER_BEHANDLING_MIN, this) { it.behandlingMin }
            .description("Minimum number of notifications per behandling")
            .register(meterRegistry)

        Gauge.builder(NOTIFICATIONS_PER_BEHANDLING_MAX, this) { it.behandlingMax }
            .description("Maximum number of notifications per behandling")
            .register(meterRegistry)

        Gauge.builder(NOTIFICATIONS_PER_BEHANDLING_AVG, this) { it.behandlingAvg }
            .description("Average number of notifications per behandling")
            .register(meterRegistry)

        Gauge.builder(NOTIFICATIONS_PER_USER_MIN, this) { it.userMin }
            .description("Minimum number of notifications per user (navIdent)")
            .register(meterRegistry)

        Gauge.builder(NOTIFICATIONS_PER_USER_MAX, this) { it.userMax }
            .description("Maximum number of notifications per user (navIdent)")
            .register(meterRegistry)

        Gauge.builder(NOTIFICATIONS_PER_USER_AVG, this) { it.userAvg }
            .description("Average number of notifications per user (navIdent)")
            .register(meterRegistry)

        logger.info("Initialized notification aggregate metric gauges")
    }

    /**
     * Calculate and update all aggregate metrics.
     * This method is called by the scheduled job.
     */
    fun updateAggregateMetrics() {
        try {
            logger.debug("Starting calculation of aggregate notification metrics")

            updateBehandlingMetrics()
            updateUserMetrics()

            logger.info(
                "Updated aggregate metrics - Behandling(min={}, max={}, avg={}), User(min={}, max={}, avg={})",
                behandlingMin, behandlingMax, behandlingAvg, userMin, userMax, userAvg
            )
        } catch (e: Exception) {
            logger.error("Failed to update aggregate notification metrics", e)
        }
    }

    private fun updateBehandlingMetrics() {
        try {
            // Query to count notifications grouped by behandlingId
            val countsByBehandling = notificationRepository.countNotificationsByBehandlingId()

            if (countsByBehandling.isEmpty()) {
                behandlingMin = 0.0
                behandlingMax = 0.0
                behandlingAvg = 0.0
                return
            }

            behandlingMin = countsByBehandling.minOrNull()?.toDouble() ?: 0.0
            behandlingMax = countsByBehandling.maxOrNull()?.toDouble() ?: 0.0
            behandlingAvg = countsByBehandling.average()

            logger.debug(
                "Behandling metrics calculated from {} behandlinger",
                countsByBehandling.size
            )
        } catch (e: Exception) {
            logger.error("Failed to update behandling metrics", e)
        }
    }

    private fun updateUserMetrics() {
        try {
            // Query to count notifications grouped by navIdent
            val countsByUser = notificationRepository.countNotificationsByNavIdent()

            if (countsByUser.isEmpty()) {
                userMin = 0.0
                userMax = 0.0
                userAvg = 0.0
                return
            }

            userMin = countsByUser.minOrNull()?.toDouble() ?: 0.0
            userMax = countsByUser.maxOrNull()?.toDouble() ?: 0.0
            userAvg = countsByUser.average()

            logger.debug(
                "User metrics calculated from {} users",
                countsByUser.size
            )
        } catch (e: Exception) {
            logger.error("Failed to update user metrics", e)
        }
    }
}