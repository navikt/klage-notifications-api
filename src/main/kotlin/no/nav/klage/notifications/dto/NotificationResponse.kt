package no.nav.klage.notifications.dto

import no.nav.klage.notifications.domain.NotificationSource
import java.time.LocalDateTime
import java.util.*

data class NotificationResponse(
    val id: UUID,
    val message: String,
    val navIdent: String,
    val read: Boolean,
    val source: NotificationSource,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val readAt: LocalDateTime?,
    val markedAsDeleted: Boolean,
)