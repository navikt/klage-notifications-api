package no.nav.klage.notifications.repository

import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.db.PostgresIntegrationTestBase
import no.nav.klage.notifications.domain.MeldingNotification
import no.nav.klage.notifications.domain.NotificationSource
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.*

@ActiveProfiles("local")
@DataJpaTest
class NotificationRepositoryCleanupTest : PostgresIntegrationTestBase() {

    @Autowired
    lateinit var testEntityManager: TestEntityManager

    @Autowired
    lateinit var notificationRepository: NotificationRepository

    @Autowired
    lateinit var meldingNotificationRepository: MeldingNotificationRepository

    @BeforeEach
    fun setup() {
        notificationRepository.deleteAll()
    }

    @Test
    fun `findByMarkedAsDeletedAndUpdatedAtBefore finds old deleted notifications`() {
        val now = LocalDateTime.now()

        // Create a notification marked as deleted 15 days ago (should be found)
        val oldDeletedNotification = createMeldingNotification(
            markedAsDeleted = true,
            updatedAt = now.minusDays(15)
        )

        // Create a notification marked as deleted 5 days ago (should NOT be found)
        val recentDeletedNotification = createMeldingNotification(
            markedAsDeleted = true,
            updatedAt = now.minusDays(5)
        )

        // Create an active notification (should NOT be found)
        val activeNotification = createMeldingNotification(
            markedAsDeleted = false,
            updatedAt = now.minusDays(20)
        )

        meldingNotificationRepository.save(oldDeletedNotification)
        meldingNotificationRepository.save(recentDeletedNotification)
        meldingNotificationRepository.save(activeNotification)
        testEntityManager.flush()
        testEntityManager.clear()

        // Find notifications older than 10 days and marked as deleted
        val cutoffDate = now.minusDays(10)
        val oldDeletedNotifications = notificationRepository.findByMarkedAsDeletedAndUpdatedAtBefore(
            markedAsDeleted = true,
            updatedAt = cutoffDate
        )

        // Verify that exactly 1 notification was found
        Assertions.assertThat(oldDeletedNotifications).hasSize(1)
        Assertions.assertThat(oldDeletedNotifications.first().id).isEqualTo(oldDeletedNotification.id)
    }

    private fun createMeldingNotification(
        markedAsDeleted: Boolean,
        updatedAt: LocalDateTime
    ): MeldingNotification {
        return MeldingNotification(
            id = UUID.randomUUID(),
            message = "Test notification",
            navIdent = "Z123456",
            read = false,
            source = NotificationSource.KABAL,
            createdAt = updatedAt,
            updatedAt = updatedAt,
            readAt = null,
            markedAsDeleted = markedAsDeleted,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = updatedAt.minusDays(1),
            behandlingId = UUID.randomUUID(),
            meldingId = UUID.randomUUID(),
            actorNavIdent = "Z999999",
            actorNavn = "Test Actor",
            saksnummer = "202312345",
            ytelse = Ytelse.OMS_OMP,
            behandlingType = Type.KLAGE
        )
    }
}