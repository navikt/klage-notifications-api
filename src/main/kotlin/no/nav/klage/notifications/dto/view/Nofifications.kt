package no.nav.klage.notifications.dto.view

import java.time.LocalDateTime
import java.util.*

enum class NotificationType {
    SYSTEM,
    LOST_ACCESS,
    MESSAGE,
    JOURNALPOST,
}

data class NavEmployee(
    val navIdent: String,
    val navn: String,
)

data class BehandlingInfo(
    val id: UUID,
    val typeId: String,
    val ytelseId: String,
    val saksnummer: String,
)

data class MessageNotification(
    val type: NotificationType,
    val id: UUID,
    val read: Boolean,
    val createdAt: LocalDateTime,
    val content: String,
    val actor: NavEmployee,
    val behandling: BehandlingInfo,
)

data class NotificationChanged(
    val id: UUID,
)

enum class Action(val lower: String) {
    CREATE("create"),
    READ("read"),
    UNREAD("unread"),
    DELETE("delete"),
}