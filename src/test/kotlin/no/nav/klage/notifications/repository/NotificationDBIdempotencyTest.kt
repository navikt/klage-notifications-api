package no.nav.klage.notifications.repository

import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.db.PostgresIntegrationTestBase
import no.nav.klage.notifications.domain.MeldingNotification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.*

@ActiveProfiles("local")
@DataJpaTest
class NotificationDBIdempotencyTest : PostgresIntegrationTestBase() {

    @Autowired
    lateinit var meldingNotificationRepository: MeldingNotificationRepository

    @Test
    fun `database enforces idempotency for MELDING notifications based on meldingId`() {
        val behandlingId = UUID.randomUUID()
        val meldingId = UUID.randomUUID()
        val navIdent = "Z123456"
        val now = LocalDateTime.now()

        val notification1 = MeldingNotification(
            id = UUID.randomUUID(),
            message = "First message",
            navIdent = navIdent,
            read = false,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = now,
            behandlingId = behandlingId,
            meldingId = meldingId,
            actorNavIdent = "Z999999",
            actorNavn = "Actor Name",
            saksnummer = "202312345",
            ytelse = Ytelse.SYK_SYK,
            behandlingType = Type.KLAGE,
        )

        meldingNotificationRepository.saveAndFlush(notification1)
        val countAfterFirst = meldingNotificationRepository.count()
        assertThat(countAfterFirst).isEqualTo(1)

        // Try to create another notification with the same meldingId but different id
        // This should fail due to unique constraint
        val notification2 = MeldingNotification(
            id = UUID.randomUUID(), // Different id
            message = "Second message", // Different message
            navIdent = navIdent,
            read = false,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = now,
            behandlingId = behandlingId,
            meldingId = meldingId, // Same meldingId - should violate constraint
            actorNavIdent = "Z999999",
            actorNavn = "Actor Name",
            saksnummer = "202312345",
            ytelse = Ytelse.SYK_SYK,
            behandlingType = Type.KLAGE,
        )

        // Attempt to save should throw constraint violation exception
        assertThrows<DataIntegrityViolationException> {
            meldingNotificationRepository.saveAndFlush(notification2)
        }
    }
}