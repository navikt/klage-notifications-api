package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.SystemNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface SystemNotificationRepository : JpaRepository<SystemNotification, UUID> {
    fun findByMarkedAsDeletedOrderByCreatedAtDesc(markedAsDeleted: Boolean): List<SystemNotification>
    fun findByMarkedAsDeletedAndUpdatedAtBefore(markedAsDeleted: Boolean, updatedAt: LocalDateTime): List<SystemNotification>
    fun findByIdIn(ids: List<UUID>): List<SystemNotification>
}