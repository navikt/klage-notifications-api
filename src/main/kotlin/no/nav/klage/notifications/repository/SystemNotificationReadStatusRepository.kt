package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.SystemNotificationReadStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SystemNotificationReadStatusRepository : JpaRepository<SystemNotificationReadStatus, UUID> {
    fun existsBySystemNotificationIdAndNavIdent(systemNotificationId: UUID, navIdent: String): Boolean
    fun findBySystemNotificationIdAndNavIdent(systemNotificationId: UUID, navIdent: String): SystemNotificationReadStatus?
    fun deleteBySystemNotificationIdAndNavIdent(systemNotificationId: UUID, navIdent: String)
}