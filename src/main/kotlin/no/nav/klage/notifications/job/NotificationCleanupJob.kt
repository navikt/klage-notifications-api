package no.nav.klage.notifications.job

import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.getLogger
import org.springframework.stereotype.Component

@Component
class NotificationCleanupJob(
    private val notificationService: NotificationService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private const val DAYS_BEFORE_DELETION = 10
    }

    /**
     * Scheduled job that permanently deletes notifications marked as deleted and older than 10 days.
     * Runs at 9:00 AM and 2:00 PM on weekdays (Monday-Friday).
     */
    //Keeping data until further notice.
//    @Scheduled(cron = "0 0 9,14 ? * MON-FRI")
//    fun cleanupOldDeletedNotifications() {
//        logger.debug("Starting cleanup of old deleted notifications (older than {} days)", DAYS_BEFORE_DELETION)
//
//        try {
//            val deletedCount = notificationService.deleteOldMarkedAsDeletedNotifications(DAYS_BEFORE_DELETION)
//            logger.debug("Cleanup job completed. Permanently deleted {} notifications", deletedCount)
//        } catch (e: Exception) {
//            logger.error("Error during notification cleanup job: ${e.message}", e)
//        }
//    }
}

