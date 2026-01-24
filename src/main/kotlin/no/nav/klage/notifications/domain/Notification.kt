package no.nav.klage.notifications.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "notificationType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = MeldingNotification::class, name = "MELDING"),
    JsonSubTypes.Type(value = LostAccessNotification::class, name = "LOST_ACCESS"),
    JsonSubTypes.Type(value = GainedAccessNotification::class, name = "GAINED_ACCESS"),
)
@Entity
@Table(name = "notifications", schema = "klage")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "notification_type")
abstract class Notification(
    @Id
    open val id: UUID = UUID.randomUUID(),

    @Column(name = "message", nullable = false)
    open var message: String,

    @Column(name = "nav_ident", nullable = false)
    open var navIdent: String,

    @Column(name = "read", nullable = false)
    open var read: Boolean,

    @Column(name = "created_at", nullable = false)
    open val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    open var updatedAt: LocalDateTime,

    @Column(name = "read_at")
    open var readAt: LocalDateTime?,

    @Column(name = "marked_as_deleted", nullable = false)
    open var markedAsDeleted: Boolean,

    @Column(name = "kafka_message_id")
    open val kafkaMessageId: UUID?,

    @Column(name = "source_created_at", nullable = false)
    open val sourceCreatedAt: LocalDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Notification) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "${this::class.simpleName}(id=$id, message='$message', navIdent='$navIdent', read=$read, createdAt=$createdAt, updatedAt=$updatedAt, readAt=$readAt, markedAsDeleted=$markedAsDeleted, sourceCreatedAt=$sourceCreatedAt)"
    }
}