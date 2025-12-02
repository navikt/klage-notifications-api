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

    @Column
    open var message: String,

    @Column
    open var navIdent: String,

    @Column
    open var read: Boolean,

    @Column
    open val createdAt: LocalDateTime,

    @Column
    open var updatedAt: LocalDateTime,

    @Column
    open var readAt: LocalDateTime?,

    @Column
    open var markedAsDeleted: Boolean,

    @Column
    open val kafkaMessageId: UUID?,

    @Column
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