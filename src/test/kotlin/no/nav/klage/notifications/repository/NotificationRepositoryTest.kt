package no.nav.klage.notifications.repository

import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.db.PostgresIntegrationTestBase
import no.nav.klage.notifications.domain.LostAccessNotification
import no.nav.klage.notifications.domain.MeldingNotification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.*


@ActiveProfiles("local")
@DataJpaTest
class NotificationRepositoryTest : PostgresIntegrationTestBase() {

    @Autowired
    lateinit var testEntityManager: TestEntityManager

    @Autowired
    lateinit var notificationRepository: NotificationRepository

    @Autowired
    lateinit var meldingNotificationRepository: MeldingNotificationRepository

    @Autowired
    lateinit var lostAccessNotificationRepository: LostAccessNotificationRepository

    @Test
    fun `persist meldingNotification works`() {
        val behandlingId = UUID.randomUUID()
        val meldingId = UUID.randomUUID()
        val navIdent = "Z123456"
        val now = LocalDateTime.now()

        val meldingNotification = MeldingNotification(
            id = UUID.randomUUID(),
            message = "You have a new message in case $behandlingId",
            navIdent = navIdent,
            read = false,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = now.minusDays(2),
            behandlingId = behandlingId,
            meldingId = meldingId,
            actorNavIdent = "Z999999",
            actorNavn = "Test Testesen",
            saksnummer = "202312345",
            ytelse = Ytelse.OMS_OMP,
            behandlingType = Type.KLAGE
        )

        val saved = meldingNotificationRepository.save(meldingNotification)
        testEntityManager.flush()
        testEntityManager.clear()

        val found = meldingNotificationRepository.findById(saved.id)
        assertThat(found).isPresent
        assertThat(found.get()).isEqualTo(saved)
        assertThat(found.get().behandlingId).isEqualTo(behandlingId)
        assertThat(found.get().meldingId).isEqualTo(meldingId)
        assertThat(found.get().actorNavIdent).isEqualTo("Z999999")
        assertThat(found.get().actorNavn).isEqualTo("Test Testesen")
        assertThat(found.get().behandlingType).isEqualTo(Type.KLAGE)
        assertThat(found.get().navIdent).isEqualTo(navIdent)
    }

    @Test
    fun `persist lostAccessNotification works`() {
        val behandlingId = UUID.randomUUID()
        val navIdent = "Z123456"
        val now = LocalDateTime.now()

        val lostAccessNotification = LostAccessNotification(
            id = UUID.randomUUID(),
            message = "You no longer have access to case $behandlingId",
            navIdent = navIdent,
            read = false,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = now.minusDays(3),
            behandlingId = behandlingId,
            saksnummer = "202398765",
            ytelse = Ytelse.SYK_SYK,
            behandlingType = Type.ANKE
        )

        val saved = lostAccessNotificationRepository.save(lostAccessNotification)
        testEntityManager.flush()
        testEntityManager.clear()

        val found = lostAccessNotificationRepository.findById(saved.id)
        assertThat(found).isPresent
        assertThat(found.get()).isEqualTo(saved)
        assertThat(found.get().behandlingId).isEqualTo(behandlingId)
        assertThat(found.get().behandlingType).isEqualTo(Type.ANKE)
        assertThat(found.get().navIdent).isEqualTo(navIdent)
    }

    @Test
    fun `findAllByBehandlingId returns both MeldingNotification and LostAccessNotification`() {
        val sharedBehandlingId = UUID.randomUUID()
        val otherBehandlingId = UUID.randomUUID()
        val now = LocalDateTime.now()

        // Create a MeldingNotification with sharedBehandlingId
        val meldingNotification = MeldingNotification(
            id = UUID.randomUUID(),
            message = "New message for behandling",
            navIdent = "Z111111",
            read = false,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = now.minusDays(1),
            behandlingId = sharedBehandlingId,
            meldingId = UUID.randomUUID(),
            actorNavIdent = "Z999999",
            actorNavn = "Actor Name",
            saksnummer = "202312345",
            ytelse = Ytelse.OMS_OMP,
            behandlingType = Type.KLAGE
        )

        // Create a LostAccessNotification with sharedBehandlingId
        val lostAccessNotification = LostAccessNotification(
            id = UUID.randomUUID(),
            message = "Lost access to behandling",
            navIdent = "Z222222",
            read = false,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = now.minusDays(2),
            behandlingId = sharedBehandlingId,
            saksnummer = "202312345",
            ytelse = Ytelse.SYK_SYK,
            behandlingType = Type.ANKE
        )

        // Create another MeldingNotification with a different behandlingId (should not be returned)
        val otherMeldingNotification = MeldingNotification(
            id = UUID.randomUUID(),
            message = "Message for other behandling",
            navIdent = "Z333333",
            read = false,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = now.minusDays(3),
            behandlingId = otherBehandlingId,
            meldingId = UUID.randomUUID(),
            actorNavIdent = "Z888888",
            actorNavn = "Other Actor",
            saksnummer = "202398765",
            ytelse = Ytelse.FOR_FOR,
            behandlingType = Type.KLAGE
        )

        meldingNotificationRepository.save(meldingNotification)
        lostAccessNotificationRepository.save(lostAccessNotification)
        meldingNotificationRepository.save(otherMeldingNotification)

        testEntityManager.flush()
        testEntityManager.clear()

        // Find all notifications for sharedBehandlingId
        val foundNotifications = notificationRepository.findAllByBehandlingId(sharedBehandlingId)

        // Verify that exactly 2 notifications are returned
        assertThat(foundNotifications).hasSize(2)

        // Verify that both types are present
        assertThat(foundNotifications).anyMatch { it is MeldingNotification }
        assertThat(foundNotifications).anyMatch { it is LostAccessNotification }

        // Verify that all returned notifications have the correct behandlingId
        assertThat(foundNotifications).allMatch { notification ->
            when (notification) {
                is MeldingNotification -> notification.behandlingId == sharedBehandlingId
                is LostAccessNotification -> notification.behandlingId == sharedBehandlingId
                else -> false
            }
        }

        // Verify that the other behandling's notification is not included
        assertThat(foundNotifications).noneMatch { it.id == otherMeldingNotification.id }
    }

}