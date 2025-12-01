package no.nav.klage.notifications.dto.view

import java.time.LocalDateTime
import java.util.*

data class SystemNotificationResponse(
    val id: UUID,
    val title: String,
    val message: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)