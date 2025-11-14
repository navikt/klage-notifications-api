package no.nav.klage.notifications.domain

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.TypeConverter
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.kodeverk.ytelse.YtelseConverter
import java.time.LocalDateTime
import java.util.*

@Entity
@DiscriminatorValue("LOST_ACCESS")
class LostAccessNotification(
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
    val saksnummer: String,

    @Column(name = "ytelse_id")
    @Convert(converter = YtelseConverter::class)
    val ytelse: Ytelse,

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
        if (other !is LostAccessNotification) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "LostAccessNotification(id=$id, message='$message', navIdent='$navIdent', read=$read, source=$source, createdAt=$createdAt, updatedAt=$updatedAt, readAt=$readAt, markedAsDeleted=$markedAsDeleted, behandlingId=$behandlingId, saksnummer='$saksnummer', ytelse=$ytelse, behandlingType=$behandlingType, kafkaMessageId=$kafkaMessageId)"
    }
}