package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.LostAccessNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface LostAccessNotificationRepository : JpaRepository<LostAccessNotification, UUID> {
    fun findByMarkedAsDeleted(markedAsDeleted: Boolean = false): List<LostAccessNotification>
}