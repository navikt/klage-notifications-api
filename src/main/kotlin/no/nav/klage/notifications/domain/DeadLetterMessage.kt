package no.nav.klage.notifications.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "dead_letter_messages", schema = "klage")
data class DeadLetterMessage(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "topic", nullable = false)
    val topic: String,

    @Column(name = "message_key")
    val messageKey: String?,

    @Column(name = "message_value", nullable = false)
    val messageValue: String,

    @Column(name = "kafka_offset", nullable = false)
    val kafkaOffset: Long,

    @Column(name = "partition", nullable = false)
    val partition: Int,

    @Column(name = "error_message")
    var errorMessage: String?,

    @Column(name = "stack_trace")
    val stackTrace: String?,

    @Column(name = "attempt_count", nullable = false)
    val attemptCount: Int,

    @Column(name = "first_attempt_at", nullable = false)
    val firstAttemptAt: LocalDateTime,

    @Column(name = "last_attempt_at", nullable = false)
    val lastAttemptAt: LocalDateTime,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "processed_at")
    var processedAt: LocalDateTime?,

    @Column(name = "processed", nullable = false)
    var processed: Boolean,

    @Column(name = "reprocess", nullable = false)
    var reprocess: Boolean,

    @Column(name = "reprocessed_at")
    var reprocessedAt: LocalDateTime?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeadLetterMessage) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "DeadLetterMessage(id=$id, topic='$topic', messageKey=$messageKey, kafkaOffset=$kafkaOffset, partition=$partition, attemptCount=$attemptCount, processed=$processed, reprocess=$reprocess)"
    }
}