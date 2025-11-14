package no.nav.klage.notifications.dto

import no.nav.klage.kodeverk.Type
import no.nav.klage.notifications.domain.NotificationSource
import java.util.*

data class CreateMeldingNotificationRequest(
    val message: String,
    val navIdent: String,
    val source: NotificationSource,
    val meldingId: UUID,
    val senderNavIdent: String,
    val behandlingId: UUID,
    val behandlingType: Type,
)