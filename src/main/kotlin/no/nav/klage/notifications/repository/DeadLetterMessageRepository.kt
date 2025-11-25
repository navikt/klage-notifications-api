package no.nav.klage.notifications.repository

import no.nav.klage.notifications.domain.DeadLetterMessage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DeadLetterMessageRepository : JpaRepository<DeadLetterMessage, UUID> {
    fun findByReprocessOrderByCreatedAtAsc(reprocess: Boolean): List<DeadLetterMessage>
}