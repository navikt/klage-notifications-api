package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.LostAccessNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LostAccessNotificationRepository : JpaRepository<LostAccessNotification, UUID>