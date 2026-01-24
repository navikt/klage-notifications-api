package no.nav.klage.notifications.domain

import com.fasterxml.jackson.annotation.JsonTypeName
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

@JsonTypeName("LOST_ACCESS")
@Entity
@DiscriminatorValue("LOST_ACCESS")
class LostAccessNotification(
    id: UUID = UUID.randomUUID(),
    message: String,
    navIdent: String,
    read: Boolean,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    readAt: LocalDateTime?,
    markedAsDeleted: Boolean,
    kafkaMessageId: UUID?,
    sourceCreatedAt: LocalDateTime,

    @Column(name = "behandling_id", nullable = false)
    val behandlingId: UUID,

    @Column(name = "saksnummer", nullable = false)
    val saksnummer: String,

    @Column(name = "ytelse_id", nullable = false)
    @Convert(converter = YtelseConverter::class)
    val ytelse: Ytelse,

    @Column(name = "behandling_type_id", nullable = false)
    @Convert(converter = TypeConverter::class)
    val behandlingType: Type,
) : Notification(
    id = id,
    message = message,
    navIdent = navIdent,
    read = read,
    createdAt = createdAt,
    updatedAt = updatedAt,
    readAt = readAt,
    markedAsDeleted = markedAsDeleted,
    kafkaMessageId = kafkaMessageId,
    sourceCreatedAt = sourceCreatedAt,
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
        return "LostAccessNotification(id=$id, message='$message', navIdent='$navIdent', read=$read, createdAt=$createdAt, updatedAt=$updatedAt, readAt=$readAt, markedAsDeleted=$markedAsDeleted, behandlingId=$behandlingId, saksnummer='$saksnummer', ytelse=$ytelse, behandlingType=$behandlingType, kafkaMessageId=$kafkaMessageId)"
    }
}