package no.nav.klage.notifications.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "system_notifications", schema = "klage")
class SystemNotification(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "title", nullable = false)
    val title: String,

    @Column(name = "message", nullable = false)
    val message: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,

    @Column(name = "marked_as_deleted", nullable = false)
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
        return "SystemNotification(id=$id, title='$title', message='$message', createdAt=$createdAt, updatedAt=$updatedAt, markedAsDeleted=$markedAsDeleted)"
    }
}