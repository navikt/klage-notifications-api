package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.SystemNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SystemNotificationRepository : JpaRepository<SystemNotification, UUID> {
    fun findByMarkedAsDeletedOrderByCreatedAtDesc(markedAsDeleted: Boolean): List<SystemNotification>
}