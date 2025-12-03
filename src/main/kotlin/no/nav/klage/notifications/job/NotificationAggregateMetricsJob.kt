package no.nav.klage.notifications.job

import no.nav.klage.notifications.service.LeaderElectionService
import no.nav.klage.notifications.service.NotificationAggregateMetricsService
import no.nav.klage.notifications.util.getLogger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class NotificationAggregateMetricsJob(
    private val notificationAggregateMetricsService: NotificationAggregateMetricsService,
    private val leaderElectionService: LeaderElectionService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    /**
     * Scheduled job that calculates and updates aggregate notification metrics.
     * Runs every second minute.
     */
    @Scheduled(timeUnit = TimeUnit.MINUTES, fixedDelay = 2, initialDelay = 1)
    fun updateAggregateMetrics() {
        logger.debug("Starting scheduled update of aggregate notification metrics")

        try {
            notificationAggregateMetricsService.updateAggregateMetrics()
            logger.debug("Successfully completed scheduled update of aggregate notification metrics")
        } catch (e: Exception) {
            logger.error("Error during scheduled update of aggregate notification metrics", e)
        }
    }

}
