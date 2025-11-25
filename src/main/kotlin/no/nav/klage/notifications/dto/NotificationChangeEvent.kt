package no.nav.klage.notifications.dto

import java.time.LocalDateTime
import java.util.*

data class NotificationChangeEvent(
    val id: UUID?,
    val ids: List<UUID>?,
    val navIdent: String,
    val type: Type,
    val updatedAt: LocalDateTime,
) {
    enum class Type {
        READ,
        READ_MULTIPLE,
        UNREAD,
        UNREAD_MULTIPLE,
        DELETED,
        DELETED_MULTIPLE,
    }
}