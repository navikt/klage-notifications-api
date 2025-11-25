package no.nav.klage.notifications.dto

import no.nav.klage.notifications.domain.Notification

data class InternalNotificationEvent(
    val notifications: List<Notification>,
)