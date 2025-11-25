package no.nav.klage.notifications.service

import no.nav.klage.notifications.domain.DeadLetterMessage
import no.nav.klage.notifications.repository.DeadLetterMessageRepository
import no.nav.klage.notifications.util.getLogger
import no.nav.klage.notifications.util.ourJacksonObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class DeadLetterQueueService(
    private val deadLetterMessageRepository: DeadLetterMessageRepository,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val objectMapper = ourJacksonObjectMapper()
    }

    fun sendToDeadLetterQueue(
        record: ConsumerRecord<String, *>,
        exception: Exception,
        attemptCount: Int,
        firstAttemptAt: LocalDateTime,
    ) {
        try {
            val messageValue = try {
                objectMapper.writeValueAsString(record.value())
            } catch (e: Exception) {
                logger.error(
                    "Could not serialize message value, using toString()",
                    e,
                )
                record.value()?.toString() ?: "null"
            }

            val stackTrace = getStackTraceAsString(exception)

            val deadLetterMessage = DeadLetterMessage(
                topic = record.topic(),
                messageKey = record.key(),
                messageValue = messageValue,
                kafkaOffset = record.offset(),
                partition = record.partition(),
                errorMessage = exception.message ?: exception::class.java.simpleName,
                stackTrace = stackTrace,
                attemptCount = attemptCount,
                firstAttemptAt = firstAttemptAt,
                lastAttemptAt = LocalDateTime.now(),
                processed = false,
                reprocess = false,
                reprocessedAt = null,
                createdAt = LocalDateTime.now(),
                processedAt = null,
            )

            deadLetterMessageRepository.save(deadLetterMessage)

            logger.error(
                "Message sent to DLQ - Topic: {}, Offset: {}, Partition: {}, Attempts: {}, Error: {}",
                record.topic(),
                record.offset(),
                record.partition(),
                attemptCount,
                exception.message,
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to save message to DLQ - Topic: {}, Offset: {}, Partition: {}",
                record.topic(),
                record.offset(),
                record.partition(),
                e,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getMessagesMarkedForReprocessing(): List<DeadLetterMessage> {
        return deadLetterMessageRepository.findByReprocessOrderByCreatedAtAsc(true)
    }

    fun markAsReprocessed(id: UUID, success: Boolean, errorMessage: String? = null) {
        val message = deadLetterMessageRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Dead letter message not found: $id") }

        message.reprocess = false
        message.reprocessedAt = LocalDateTime.now()

        if (success) {
            message.processed = true
            message.processedAt = LocalDateTime.now()
            logger.info(
                "Successfully reprocessed DLQ message {} - Topic: {}, Offset: {}",
                id,
                message.topic,
                message.kafkaOffset,
            )
        } else {
            message.errorMessage = errorMessage ?: message.errorMessage
            logger.warn(
                "Failed to reprocess DLQ message {} - Topic: {}, Offset: {}, Error: {}",
                id,
                message.topic,
                message.kafkaOffset,
                errorMessage,
            )
        }
    }

    private fun getStackTraceAsString(exception: Exception): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        exception.printStackTrace(pw)
        return sw.toString()
    }
}