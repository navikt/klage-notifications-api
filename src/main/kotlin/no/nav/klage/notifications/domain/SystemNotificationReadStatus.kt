package no.nav.klage.notifications.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "system_notification_read_status", schema = "klage")
class SystemNotificationReadStatus(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "system_notification_id")
    val systemNotificationId: UUID,

    @Column(name = "nav_ident")
    val navIdent: String,

    @Column(name = "read_at")
    val readAt: LocalDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SystemNotificationReadStatus) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "SystemNotificationReadStatus(id=$id, systemNotificationId=$systemNotificationId, navIdent='$navIdent', readAt=$readAt)"
    }
}