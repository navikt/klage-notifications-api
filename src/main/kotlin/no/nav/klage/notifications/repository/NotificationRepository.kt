package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findByNavIdentAndMarkedAsDeletedOrderBySourceCreatedAtAsc(
        navIdent: String,
        markedAsDeleted: Boolean = false,
    ): List<Notification>

    fun findByNavIdentAndRead(navIdent: String, read: Boolean): List<Notification>
}