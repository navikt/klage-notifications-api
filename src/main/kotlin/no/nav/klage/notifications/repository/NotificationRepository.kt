package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findByNavIdentAndMarkedAsDeletedOrderBySourceCreatedAtAsc(
        navIdent: String,
        markedAsDeleted: Boolean = false,
    ): List<Notification>

    fun findByNavIdentAndRead(navIdent: String, read: Boolean): List<Notification>

    @Query("""
        SELECT n FROM Notification n 
        WHERE (TYPE(n) = MeldingNotification AND TREAT(n AS MeldingNotification).behandlingId = :behandlingId)
        OR (TYPE(n) = LostAccessNotification AND TREAT(n AS LostAccessNotification).behandlingId = :behandlingId)
    """)
    fun findAllByBehandlingId(behandlingId: UUID): List<Notification>

    fun findByMarkedAsDeletedAndUpdatedAtBefore(
        markedAsDeleted: Boolean,
        updatedAt: LocalDateTime
    ): List<Notification>
}