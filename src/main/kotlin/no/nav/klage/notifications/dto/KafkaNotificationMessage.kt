package no.nav.klage.notifications.dto

import no.nav.klage.notifications.domain.NotificationSource

data class KafkaNotificationMessage(
    val message: String,
    val navIdent: String,
    val source: NotificationSource,
)