package no.nav.klage.notifications.domain

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.TypeConverter
import java.time.LocalDateTime
import java.util.*

@Entity
@DiscriminatorValue("MELDING")
class MeldingNotification(
    id: UUID = UUID.randomUUID(),
    message: String,
    navIdent: String,
    read: Boolean,
    source: NotificationSource,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    readAt: LocalDateTime?,
    markedAsDeleted: Boolean,
    kafkaMessageId: UUID?,

    @Column
    val behandlingId: UUID,

    @Column
    val meldingId: UUID,

    @Column
    val senderNavIdent: String,

    @Column(name = "behandling_type_id")
    @Convert(converter = TypeConverter::class)
    val behandlingType: Type,
) : Notification(
    id = id,
    message = message,
    navIdent = navIdent,
    read = read,
    source = source,
    createdAt = createdAt,
    updatedAt = updatedAt,
    readAt = readAt,
    markedAsDeleted = markedAsDeleted,
    kafkaMessageId = kafkaMessageId,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeldingNotification) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "MeldingNotification(id=$id, message='$message', navIdent='$navIdent', read=$read, source=$source, createdAt=$createdAt, updatedAt=$updatedAt, readAt=$readAt, markedAsDeleted=$markedAsDeleted, behandlingId=$behandlingId, meldingId=$meldingId, senderNavIdent='$senderNavIdent', behandlingType=$behandlingType, kafkaMessageId=$kafkaMessageId)"
    }
}