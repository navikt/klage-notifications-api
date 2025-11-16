package no.nav.klage.notifications.dto

import java.time.LocalDateTime
import java.util.*

data class NotificationChangeEvent(
    val id: UUID,
    val navIdent: String,
    val type: Type,
    val updatedAt: LocalDateTime,
) {
    enum class Type {
        READ,
        UNREAD,
        DELETED,
    }
}