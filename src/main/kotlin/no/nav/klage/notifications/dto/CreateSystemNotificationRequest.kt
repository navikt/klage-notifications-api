package no.nav.klage.notifications.dto

import no.nav.klage.notifications.domain.NotificationSource

data class CreateSystemNotificationRequest(
    val title: String,
    val message: String,
    val source: NotificationSource,
)