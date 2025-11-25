package no.nav.klage.notifications.service

import no.nav.klage.notifications.domain.DeadLetterMessage
import no.nav.klage.notifications.dto.CreateNotificationEvent
import no.nav.klage.notifications.util.getLogger
import no.nav.klage.notifications.util.ourJacksonObjectMapper
import org.springframework.stereotype.Service
import java.util.*

@Service
class DLQReprocessingService(
    private val deadLetterQueueService: DeadLetterQueueService,
    private val notificationService: NotificationService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val objectMapper = ourJacksonObjectMapper()
    }

    fun reprocessMessages() {
        val messagesToReprocess = deadLetterQueueService.getMessagesMarkedForReprocessing()

        if (messagesToReprocess.isEmpty()) {
            logger.debug("No DLQ messages marked for reprocessing")
            return
        }

        logger.info(
            "Found {} DLQ messages marked for reprocessing",
            messagesToReprocess.size,
        )

        messagesToReprocess.forEach { dlqMessage ->
            reprocessMessage(dlqMessage)
        }
    }

    private fun reprocessMessage(dlqMessage: DeadLetterMessage) {
        try {
            logger.info(
                "Reprocessing DLQ message {} - Topic: {}, Offset: {}, Partition: {}",
                dlqMessage.id,
                dlqMessage.topic,
                dlqMessage.kafkaOffset,
                dlqMessage.partition,
            )

            // Deserialize the message value back to CreateNotificationEvent
            val createNotificationEvent = objectMapper.readValue(
                dlqMessage.messageValue,
                CreateNotificationEvent::class.java,
            )

            // Get the Kafka message ID from the message key
            val kafkaMessageId = UUID.fromString(dlqMessage.messageKey)

            // Process the notification
            notificationService.processNotificationMessage(
                kafkaMessageId = kafkaMessageId,
                createNotificationEvent = createNotificationEvent,
            )

            // Mark as successfully reprocessed
            deadLetterQueueService.markAsReprocessed(
                dlqMessage.id,
                success = true,
            )

            logger.info(
                "Successfully reprocessed DLQ message {} - Topic: {}, Offset: {}",
                dlqMessage.id,
                dlqMessage.topic,
                dlqMessage.kafkaOffset,
            )

        } catch (e: Exception) {
            logger.error(
                "Failed to reprocess DLQ message {} - Topic: {}, Offset: {}: {}",
                dlqMessage.id,
                dlqMessage.topic,
                dlqMessage.kafkaOffset,
                e.message,
                e,
            )

            // Mark as failed reprocessing
            deadLetterQueueService.markAsReprocessed(
                dlqMessage.id,
                success = false,
                errorMessage = "Reprocessing failed: ${e.message}",
            )
        }
    }
}