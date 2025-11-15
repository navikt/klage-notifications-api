package no.nav.klage.notifications.dto

import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.domain.NotificationSource
import no.nav.klage.notifications.domain.NotificationType
import java.time.LocalDateTime
import java.util.*

data class CreateLostAccessNotificationRequest(
    val type: NotificationType,
    val message: String,
    val recipientNavIdent: String,
    val source: NotificationSource,
    val behandlingId: UUID,
    val saksnummer: String,
    val ytelse: Ytelse,
    val behandlingType: Type,
    val sourceCreatedAt: LocalDateTime?,
)