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

    @Column
    val topic: String,

    @Column
    val messageKey: String?,

    @Column
    val messageValue: String,

    @Column(name = "kafka_offset")
    val kafkaOffset: Long,

    @Column
    val partition: Int,

    @Column
    var errorMessage: String?,

    @Column
    val stackTrace: String?,

    @Column
    val attemptCount: Int,

    @Column
    val firstAttemptAt: LocalDateTime,

    @Column
    val lastAttemptAt: LocalDateTime,

    @Column
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var processedAt: LocalDateTime?,

    @Column
    var processed: Boolean,

    @Column
    var reprocess: Boolean,

    @Column
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

