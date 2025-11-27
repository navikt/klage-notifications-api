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

    //Need leader election to enable scheduling in a clustered environment (or similar)

    /**
     * Scheduled job that calculates and updates aggregate notification metrics.
     * Runs every day at 3 AM.
     *
     * Cron format: second, minute, hour, day of month, month, day of week
     * "0 0 3 * * *" = At 3:00:00 AM every day
     */
    @Scheduled(cron = "0 * * * * *")
    fun updateAggregateMetrics() {
        if (leaderElectionService.isLeader()) {
            logger.debug("Not the leader instance, skipping scheduled update of aggregate notification metrics")
            return
        } else {
            logger.debug("This is the leader instance, proceeding with scheduled update of aggregate notification metrics")
        }

        logger.debug("Starting scheduled update of aggregate notification metrics")
//
//        try {
//            notificationAggregateMetricsService.updateAggregateMetrics()
//            logger.info("Successfully completed scheduled update of aggregate notification metrics")
//        } catch (e: Exception) {
//            logger.error("Error during scheduled update of aggregate notification metrics", e)
//        }
    }
//
//    /**
//     * Also update metrics every 6 hours during the day for more frequent updates.
//     * Runs at 00:00, 06:00, 12:00, and 18:00.
//     */
//    @Scheduled(cron = "0 0 */6 * * *")
//    fun updateAggregateMetricsFrequently() {
//        logger.debug("Starting frequent update of aggregate notification metrics")
//
//        try {
//            notificationAggregateMetricsService.updateAggregateMetrics()
//            logger.debug("Successfully completed frequent update of aggregate notification metrics")
//        } catch (e: Exception) {
//            logger.error("Error during frequent update of aggregate notification metrics", e)
//        }
//    }
}
