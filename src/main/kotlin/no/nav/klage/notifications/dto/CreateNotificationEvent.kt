package no.nav.klage.notifications.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.domain.NotificationType
import java.time.LocalDateTime
import java.util.*


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = CreateMeldingNotificationEvent::class, name = "MELDING"),
    JsonSubTypes.Type(value = CreateLostAccessNotificationRequest::class, name = "LOST_ACCESS"),
    JsonSubTypes.Type(value = CreateGainedAccessNotificationRequest::class, name = "GAINED_ACCESS"),
)
@JsonIgnoreProperties(ignoreUnknown = true)
sealed class CreateNotificationEvent(
    open val type: NotificationType,
    open val message: String,
    open val recipientNavIdent: String,
    open val sourceCreatedAt: LocalDateTime,
    open val actorNavIdent: String,
    open val actorNavn: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateMeldingNotificationEvent(
    override val type: NotificationType,
    override val message: String,
    override val recipientNavIdent: String,
    override val actorNavIdent: String,
    override val actorNavn: String,
    override val sourceCreatedAt: LocalDateTime,
    val meldingId: UUID,
    val behandlingId: UUID,
    val behandlingType: Type,
    val saksnummer: String,
    val ytelse: Ytelse,
) : CreateNotificationEvent(
    type = type,
    message = message,
    recipientNavIdent = recipientNavIdent,
    sourceCreatedAt = sourceCreatedAt,
    actorNavIdent = actorNavIdent,
    actorNavn = actorNavn,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateLostAccessNotificationRequest(
    override val type: NotificationType,
    override val message: String,
    override val recipientNavIdent: String,
    override val actorNavIdent: String,
    override val actorNavn: String,
    override val sourceCreatedAt: LocalDateTime,
    val behandlingId: UUID,
    val behandlingType: Type,
    val saksnummer: String,
    val ytelse: Ytelse,
) : CreateNotificationEvent(
    type = type,
    message = message,
    recipientNavIdent = recipientNavIdent,
    sourceCreatedAt = sourceCreatedAt,
    actorNavIdent = actorNavIdent,
    actorNavn = actorNavn,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateGainedAccessNotificationRequest(
    override val type: NotificationType,
    override val message: String,
    override val recipientNavIdent: String,
    override val actorNavIdent: String,
    override val actorNavn: String,
    override val sourceCreatedAt: LocalDateTime,
    val behandlingId: UUID,
    val behandlingType: Type,
    val saksnummer: String,
    val ytelse: Ytelse,
) : CreateNotificationEvent(
    type = type,
    message = message,
    recipientNavIdent = recipientNavIdent,
    sourceCreatedAt = sourceCreatedAt,
    actorNavIdent = actorNavIdent,
    actorNavn = actorNavn,
)