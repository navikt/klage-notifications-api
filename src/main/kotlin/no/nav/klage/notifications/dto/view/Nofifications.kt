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

abstract class Notification(
    open val type: NotificationType,
    open val id: UUID,
    open val read: Boolean,
    open val createdAt: LocalDateTime,
)

data class MessageNotification(
    override val type: NotificationType,
    override val id: UUID,
    override val read: Boolean,
    override val createdAt: LocalDateTime,
    val message: Message,
    val actor: NavEmployee,
    val behandling: BehandlingInfo,
): Notification(
    type = type,
    id = id,
    read = read,
    createdAt = createdAt,
) {
    data class Message(
        val id: UUID,
        val content: String,
    )
}

data class SystemNotificationView(
    override val type: NotificationType,
    override val id: UUID,
    override val read: Boolean,
    override val createdAt: LocalDateTime,
    val title: String,
    val message: String,
): Notification(
    type = type,
    id = id,
    read = read,
    createdAt = createdAt,
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