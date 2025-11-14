package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findByNavIdent(navIdent: String): List<Notification>
    fun findByNavIdentAndReadOrderByCreatedAtAsc(navIdent: String, read: Boolean): List<Notification>
    fun findByNavIdentOrderByCreatedAtAsc(navIdent: String): List<Notification>
}