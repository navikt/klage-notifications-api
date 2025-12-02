package no.nav.klage.notifications.dto.view

import java.time.LocalDateTime
import java.util.*

enum class NotificationType {
    SYSTEM,
    LOST_ACCESS,
    GAINED_ACCESS,
    MESSAGE,
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

abstract class NotificationView(
    open val type: NotificationType,
    open val id: UUID,
    open val read: Boolean,
    open val createdAt: LocalDateTime,
)

data class MessageNotificationView(
    override val type: NotificationType,
    override val id: UUID,
    override val read: Boolean,
    override val createdAt: LocalDateTime,
    val message: Message,
    val actor: NavEmployee,
    val behandling: BehandlingInfo,
): NotificationView(
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

data class LostAccessNotificationView(
    override val type: NotificationType,
    override val id: UUID,
    override val read: Boolean,
    override val createdAt: LocalDateTime,
    val message: String,
    val behandling: BehandlingInfo,
): NotificationView(
    type = type,
    id = id,
    read = read,
    createdAt = createdAt,
)

data class GainedAccessNotificationView(
    override val type: NotificationType,
    override val id: UUID,
    override val read: Boolean,
    override val createdAt: LocalDateTime,
    val message: String,
    val behandling: BehandlingInfo,
): NotificationView(
    type = type,
    id = id,
    read = read,
    createdAt = createdAt,
)

data class SystemNotificationView(
    override val type: NotificationType,
    override val id: UUID,
    override val read: Boolean,
    override val createdAt: LocalDateTime,
    val title: String,
    val message: String,
): NotificationView(
    type = type,
    id = id,
    read = read,
    createdAt = createdAt,
)

data class NotificationChanged(
    val id: UUID,
)

data class NotificationMultipleChanged(
    val ids: List<UUID>,
)

enum class Action(val lower: String) {
    CREATE("create"),
    CREATE_MULTIPLE("create_multiple"),
    READ("read"),
    READ_MULTIPLE("read_multiple"),
    UNREAD("unread"),
    UNREAD_MULTIPLE("unread_multiple"),
    DELETE("delete"),
    DELETE_MULTIPLE("delete_multiple"),
}