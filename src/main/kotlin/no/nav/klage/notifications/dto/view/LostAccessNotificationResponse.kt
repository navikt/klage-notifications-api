package no.nav.klage.notifications.dto.view

import java.util.*

data class LostAccessNotificationResponse(
    val behandlingId: UUID,
    val navIdent: String,
)