package no.nav.klage.notifications.dto

import no.nav.klage.notifications.domain.NotificationStatus
import no.nav.klage.notifications.domain.NotificationSeverity

data class UpdateNotificationRequest(
    val title: String? = null,
    val message: String? = null,
    val severity: NotificationSeverity? = null,
    val status: NotificationStatus? = null,
)