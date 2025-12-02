package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.GainedAccessNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GainedAccessNotificationRepository : JpaRepository<GainedAccessNotification, UUID> {
    fun findByBehandlingIdAndNavIdentAndMarkedAsDeleted(
        behandlingId: UUID,
        navIdent: String,
        markedAsDeleted: Boolean = false
    ): GainedAccessNotification?

    fun findByMarkedAsDeleted(markedAsDeleted: Boolean = false): List<GainedAccessNotification>
}