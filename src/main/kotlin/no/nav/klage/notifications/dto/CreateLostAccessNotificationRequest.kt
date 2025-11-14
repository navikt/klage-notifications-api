package no.nav.klage.notifications.dto

import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.domain.NotificationSource
import java.util.*

data class CreateLostAccessNotificationRequest(
    val message: String,
    val navIdent: String,
    val source: NotificationSource,
    val behandlingId: UUID,
    val saksnummer: String,
    val ytelse: Ytelse,
    val behandlingType: Type,
)