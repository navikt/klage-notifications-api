package no.nav.klage.notifications.dto

import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.domain.NotificationSource
import no.nav.klage.notifications.domain.NotificationType
import java.time.LocalDateTime
import java.util.*

sealed class CreateNotificationEvent(
    open val type: NotificationType,
    open val message: String,
    open val recipientNavIdent: String,
    open val source: NotificationSource,
    open val sourceCreatedAt: LocalDateTime,
    open val actorNavIdent: String,
    open val actorNavn: String,
)

data class CreateMeldingNotificationEvent(
    override val type: NotificationType,
    override val message: String,
    override val recipientNavIdent: String,
    override val source: NotificationSource,
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
    source = source,
    sourceCreatedAt = sourceCreatedAt,
    actorNavIdent = actorNavIdent,
    actorNavn = actorNavn,
)

data class CreateLostAccessNotificationRequest(
    override val type: NotificationType,
    override val message: String,
    override val recipientNavIdent: String,
    override val source: NotificationSource,
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
    source = source,
    sourceCreatedAt = sourceCreatedAt,
    actorNavIdent = actorNavIdent,
    actorNavn = actorNavn,
)