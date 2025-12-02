package no.nav.klage.notifications.job

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.klage.notifications.service.DLQReprocessingService
import no.nav.klage.notifications.util.getLogger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DLQReprocessingJob(
    private val dlqReprocessingService: DLQReprocessingService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    /**
     * Scheduled job that checks for DLQ messages marked for reprocessing.
     * Runs every 5 minutes.
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    @SchedulerLock(name = "reprocessMarkedMessages")
    fun reprocessMarkedMessages() {
        logger.debug("Starting DLQ reprocessing job")

        try {
            dlqReprocessingService.reprocessMessages()
            logger.debug("DLQ reprocessing job completed")
        } catch (e: Exception) {
            logger.error("Error during DLQ reprocessing job: ${e.message}", e)
        }
    }
}