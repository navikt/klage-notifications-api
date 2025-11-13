package no.nav.klage.notifications.domain

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.TypeConverter
import java.time.LocalDateTime
import java.util.*

@Entity
@DiscriminatorValue("LOST_ACCESS")
class LostAccessNotification(
    id: UUID = UUID.randomUUID(),
    title: String,
    message: String,
    navIdent: String,
    severity: NotificationSeverity,
    status: NotificationStatus,
    source: NotificationSource,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    readAt: LocalDateTime?,
    markedAsDeleted: Boolean,

    @Column
    val behandlingId: UUID,

    @Column(name = "behandling_type_id")
    @Convert(converter = TypeConverter::class)
    val behandlingType: Type,
) : Notification(
    id = id,
    title = title,
    message = message,
    navIdent = navIdent,
    severity = severity,
    status = status,
    source = source,
    createdAt = createdAt,
    updatedAt = updatedAt,
    readAt = readAt,
    markedAsDeleted = markedAsDeleted,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LostAccessNotification) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "LostAccessNotification(id=$id, title='$title', message='$message', navIdent='$navIdent', severity=$severity, status=$status, source=$source, createdAt=$createdAt, updatedAt=$updatedAt, readAt=$readAt, markedAsDeleted=$markedAsDeleted, behandlingId=$behandlingId, behandlingType=$behandlingType)"
    }
}