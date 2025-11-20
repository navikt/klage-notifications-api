package no.nav.klage.notifications.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "system_notifications", schema = "klage")
class SystemNotification(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column
    val title: String,

    @Column
    val message: String,

    @Enumerated(EnumType.STRING)
    @Column
    val source: NotificationSource,

    @Column
    val createdAt: LocalDateTime,

    @Column
    var updatedAt: LocalDateTime,

    @Column
    var markedAsDeleted: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SystemNotification) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "SystemNotification(id=$id, title='$title', message='$message', source=$source, createdAt=$createdAt, updatedAt=$updatedAt, markedAsDeleted=$markedAsDeleted)"
    }
}