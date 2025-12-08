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

    @Query("""
        SELECT n FROM Notification n 
        WHERE n.read = :read 
        AND n.markedAsDeleted = false
        AND ((TYPE(n) = MeldingNotification AND TREAT(n AS MeldingNotification).behandlingId = :behandlingId)
        OR (TYPE(n) = LostAccessNotification AND TREAT(n AS LostAccessNotification).behandlingId = :behandlingId))
    """)
    fun findByReadAndBehandlingIdAndNotMarkedAsDeleted(read: Boolean, behandlingId: UUID): List<Notification>

    fun findByMarkedAsDeletedAndUpdatedAtBefore(
        markedAsDeleted: Boolean,
        updatedAt: LocalDateTime
    ): List<Notification>

    fun findByIdInAndNavIdent(ids: List<UUID>, navIdent: String): List<Notification>

    /**
     * Get count of notifications grouped by behandlingId.
     * Returns a list of counts (one per behandlingId).
     * Used for calculating min/max/avg notifications per behandling.
     */
    @Query("""
        SELECT COUNT(n) 
        FROM Notification n 
        WHERE (TYPE(n) = MeldingNotification OR TYPE(n) = LostAccessNotification)
        AND n.markedAsDeleted = false
        GROUP BY 
            CASE 
                WHEN TYPE(n) = MeldingNotification THEN TREAT(n AS MeldingNotification).behandlingId
                WHEN TYPE(n) = LostAccessNotification THEN TREAT(n AS LostAccessNotification).behandlingId
            END
    """)
    fun countNotificationsByBehandlingId(): List<Long>

    /**
     * Get count of notifications grouped by navIdent.
     * Returns a list of counts (one per user).
     * Used for calculating min/max/avg notifications per user.
     */
    @Query("""
        SELECT COUNT(n) 
        FROM Notification n 
        WHERE n.markedAsDeleted = false
        GROUP BY n.navIdent
    """)
    fun countNotificationsByNavIdent(): List<Long>

    /**
     * Find the latest access notification (LOST_ACCESS or GAINED_ACCESS) for a given behandlingId and navIdent.
     * Used to determine if a new access notification should be allowed based on the previous one.
     */
    @Query("""
        SELECT n FROM Notification n 
        WHERE n.navIdent = :navIdent
        AND n.markedAsDeleted = false
        AND ((TYPE(n) = LostAccessNotification AND TREAT(n AS LostAccessNotification).behandlingId = :behandlingId)
        OR (TYPE(n) = GainedAccessNotification AND TREAT(n AS GainedAccessNotification).behandlingId = :behandlingId))
        ORDER BY n.createdAt DESC
        LIMIT 1
    """)
    fun findLatestAccessNotificationByBehandlingIdAndNavIdent(
        behandlingId: UUID,
        navIdent: String,
    ): Notification?
}