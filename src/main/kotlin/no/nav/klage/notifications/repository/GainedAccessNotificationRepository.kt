package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.GainedAccessNotification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GainedAccessNotificationRepository : JpaRepository<GainedAccessNotification, UUID>