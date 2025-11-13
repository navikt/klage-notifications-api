package no.nav.klage.notifications.dto

import no.nav.klage.notifications.domain.NotificationSource
import no.nav.klage.notifications.domain.NotificationStatus
import no.nav.klage.notifications.domain.NotificationSeverity
import java.time.LocalDateTime
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val title: String,
    val message: String,
    val navIdent: String,
    val severity: NotificationSeverity,
    val status: NotificationStatus,
    val source: NotificationSource,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val readAt: LocalDateTime?,
    val markedAsDeleted: Boolean,
)