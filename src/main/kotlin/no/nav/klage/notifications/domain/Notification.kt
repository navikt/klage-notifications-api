package no.nav.klage.notifications.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

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

    @Enumerated(EnumType.STRING)
    @Column
    open var source: NotificationSource,

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
    open val sourceCreatedAt: LocalDateTime?,
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
        return "${this::class.simpleName}(id=$id, message='$message', navIdent='$navIdent', read=$read, source=$source, createdAt=$createdAt, updatedAt=$updatedAt, readAt=$readAt, markedAsDeleted=$markedAsDeleted, sourceCreatedAt=$sourceCreatedAt)"
    }
}