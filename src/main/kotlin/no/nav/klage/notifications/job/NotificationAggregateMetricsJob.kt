package no.nav.klage.notifications.job

import no.nav.klage.notifications.service.LeaderElectionService
import no.nav.klage.notifications.service.NotificationAggregateMetricsService
import no.nav.klage.notifications.util.getLogger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

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
     * Runs every 30 minutes.
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    fun updateAggregateMetrics() {
        if (!leaderElectionService.isLeader()) {
            logger.debug("Not the leader instance, skipping scheduled update of aggregate notification metrics")
            return
        }

        logger.debug("Starting scheduled update of aggregate notification metrics")

        try {
            notificationAggregateMetricsService.updateAggregateMetrics()
            logger.debug("Successfully completed scheduled update of aggregate notification metrics")
        } catch (e: Exception) {
            logger.error("Error during scheduled update of aggregate notification metrics", e)
        }
    }

}
