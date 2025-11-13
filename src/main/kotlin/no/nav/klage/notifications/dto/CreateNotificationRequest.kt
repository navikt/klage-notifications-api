package no.nav.klage.notifications.dto

import no.nav.klage.notifications.domain.NotificationSource
import no.nav.klage.notifications.domain.NotificationSeverity

data class CreateNotificationRequest(
    val title: String,
    val message: String,
    val navIdent: String,
    val severity: NotificationSeverity,
    val source: NotificationSource,
)