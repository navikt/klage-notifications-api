package no.nav.klage.notifications.dto

import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.domain.NotificationSource
import no.nav.klage.notifications.domain.NotificationType
import java.time.LocalDateTime
import java.util.*

data class CreateMeldingNotificationEvent(
    val type: NotificationType,
    val message: String,
    val recipientNavIdent: String,
    val source: NotificationSource,
    val meldingId: UUID,
    val behandlingId: UUID,
    val behandlingType: Type,
    val actorNavIdent: String,
    val actorNavn: String,
    val saksnummer: String,
    val ytelse: Ytelse,
    val sourceCreatedAt: LocalDateTime,
)