package no.nav.klage.notifications.dto.view

import no.nav.klage.notifications.domain.NotificationSource
import java.time.LocalDateTime
import java.util.*

data class SystemNotificationResponse(
    val id: UUID,
    val title: String,
    val message: String,
    val source: NotificationSource,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)