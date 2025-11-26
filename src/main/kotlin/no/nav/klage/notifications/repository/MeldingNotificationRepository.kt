package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.MeldingNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MeldingNotificationRepository : JpaRepository<MeldingNotification, UUID> {
    fun countByBehandlingIdAndReadAndMarkedAsDeleted(
        behandlingId: UUID,
        read: Boolean,
        markedAsDeleted: Boolean
    ): Long
}