package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.Notification
import no.nav.klage.notifications.domain.NotificationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findByNavIdent(navIdent: String): List<Notification>
    fun findByNavIdentAndStatusOrderByCreatedAtAsc(navIdent: String, status: NotificationStatus): List<Notification>
    fun findByNavIdentOrderByCreatedAtAsc(navIdent: String): List<Notification>
}