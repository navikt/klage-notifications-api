package no.nav.klage.notifications.dto

import java.time.LocalDateTime
import java.util.*

data class NotificationResponse(
    val id: UUID,
    val message: String,
    val navIdent: String,
    val read: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val readAt: LocalDateTime?,
    val markedAsDeleted: Boolean,
)