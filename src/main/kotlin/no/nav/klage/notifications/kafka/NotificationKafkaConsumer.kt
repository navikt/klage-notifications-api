package no.nav.klage.notifications.kafka

import no.nav.klage.notifications.dto.CreateNotificationEvent
import no.nav.klage.notifications.service.DeadLetterQueueService
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.getLogger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class NotificationKafkaConsumer(
    private val notificationService: NotificationService,
    private val deadLetterQueueService: DeadLetterQueueService,
    private val environment: Environment,
    @Value($$"${dlq.max-retry-attempts:3}")
    private val maxRetryAttempts: Int,
    @Value($$"${dlq.retry-delay-seconds:5}")
    private val retryDelaySeconds: Long,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    // Track retry attempts per message (topic-partition-offset)
    private val retryAttempts = ConcurrentHashMap<String, RetryInfo>()

    private data class RetryInfo(
        val attemptCount: Int,
        val firstAttemptAt: LocalDateTime,
    )

    @KafkaListener(
        topics = [$$"${NOTIFICATION_EVENTS_TOPIC}"],
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun listen(
        consumerRecord: ConsumerRecord<String, CreateNotificationEvent>,
        acknowledgment: Acknowledgment,
    ) {
        processMessage(
            consumerRecord,
            acknowledgment,
        )
    }

    private fun processMessage(
        consumerRecord: ConsumerRecord<String, CreateNotificationEvent>,
        acknowledgment: Acknowledgment,
    ) {
        val messageKey = createMessageKey(consumerRecord)
        val retryInfo = retryAttempts.getOrDefault(
            messageKey,
            RetryInfo(
                0,
                LocalDateTime.now(),
            ),
        )

        try {
            if (!environment.activeProfiles.contains("prod")) {
                logger.debug(
                    "Received Kafka-message (for persisting notification events) at offset {}: {} (attempt {}/{})",
                    consumerRecord.offset(),
                    consumerRecord.value(),
                    retryInfo.attemptCount + 1,
                    maxRetryAttempts,
                )
            } else {
                logger.debug(
                    "Received Kafka-message (for persisting notification events) at offset {} (attempt {}/{})",
                    consumerRecord.offset(),
                    retryInfo.attemptCount + 1,
                    maxRetryAttempts,
                )
            }

            notificationService.processNotificationMessage(
                kafkaMessageId = UUID.fromString(consumerRecord.key()!!),
                createNotificationEvent = consumerRecord.value(),
            )

            // Success - acknowledge and remove from retry tracking
            acknowledgment.acknowledge()
            retryAttempts.remove(messageKey)
            logger.debug(
                "Successfully processed and acknowledged message at offset {}",
                consumerRecord.offset(),
            )

        } catch (e: Exception) {
            handleProcessingError(
                consumerRecord = consumerRecord,
                acknowledgment = acknowledgment,
                exception = e,
                retryInfo = retryInfo,
                messageKey = messageKey,
            )
        }
    }

    private fun handleProcessingError(
        consumerRecord: ConsumerRecord<String, CreateNotificationEvent>,
        acknowledgment: Acknowledgment,
        exception: Exception,
        retryInfo: RetryInfo,
        messageKey: String,
    ) {
        val newAttemptCount = retryInfo.attemptCount + 1

        logger.error(
            "Error processing notification message at offset {} (attempt {}/{}): {}",
            consumerRecord.offset(),
            newAttemptCount,
            maxRetryAttempts,
            exception.message,
            exception,
        )

        if (newAttemptCount >= maxRetryAttempts) {
            // Max retries reached - send to DLQ and acknowledge to prevent infinite loop
            logger.error(
                "Max retry attempts ({}) reached for message at offset {}. Sending to DLQ.",
                maxRetryAttempts,
                consumerRecord.offset(),
            )

            try {
                deadLetterQueueService.sendToDeadLetterQueue(
                    record = consumerRecord,
                    exception = exception,
                    attemptCount = newAttemptCount,
                    firstAttemptAt = retryInfo.firstAttemptAt,
                )

                // Acknowledge the message to prevent reprocessing
                acknowledgment.acknowledge()
                retryAttempts.remove(messageKey)

                logger.info(
                    "Message at offset {} acknowledged after being sent to DLQ",
                    consumerRecord.offset(),
                )
            } catch (dlqException: Exception) {
                logger.error(
                    "Failed to send message to DLQ at offset {}. Message will be retried.",
                    consumerRecord.offset(),
                    dlqException,
                )
                // Don't acknowledge - will retry in next poll
            }
        } else {
            // Update retry count and don't acknowledge - message will be reprocessed
            retryAttempts[messageKey] = RetryInfo(
                newAttemptCount,
                retryInfo.firstAttemptAt,
            )
            logger.warn(
                "Will retry message at offset {} after delay (attempt {}/{})",
                consumerRecord.offset(),
                newAttemptCount,
                maxRetryAttempts,
            )

            // Add a small delay before next attempt
            try {
                Thread.sleep(Duration.ofSeconds(retryDelaySeconds).toMillis())
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun createMessageKey(record: ConsumerRecord<String, CreateNotificationEvent>): String {
        return "${record.topic()}-${record.partition()}-${record.offset()}"
    }
}