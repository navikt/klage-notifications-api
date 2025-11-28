package no.nav.klage.notifications.repository

import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.db.PostgresIntegrationTestBase
import no.nav.klage.notifications.domain.LostAccessNotification
import no.nav.klage.notifications.domain.MeldingNotification
import org.assertj.core.api.Assertions.assertThat
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
class NotificationRepositoryValidationTest : PostgresIntegrationTestBase() {

    @Autowired
    lateinit var testEntityManager: TestEntityManager

    @Autowired
    lateinit var notificationRepository: NotificationRepository

    @Autowired
    lateinit var meldingNotificationRepository: MeldingNotificationRepository

    @Autowired
    lateinit var lostAccessNotificationRepository: LostAccessNotificationRepository

    @BeforeEach
    fun setup() {
        notificationRepository.deleteAll()
    }

    @Test
    fun `findByReadAndBehandlingIdAndNotMarkedAsDeleted finds unread MeldingNotifications`() {
        val behandlingId = UUID.randomUUID()
        val now = LocalDateTime.now()

        // Create an unread MeldingNotification
        val unreadMelding = createMeldingNotification(
            behandlingId = behandlingId,
            read = false,
            markedAsDeleted = false,
            updatedAt = now
        )

        // Create a read MeldingNotification (should NOT be found)
        val readMelding = createMeldingNotification(
            behandlingId = behandlingId,
            read = true,
            markedAsDeleted = false,
            updatedAt = now
        )

        meldingNotificationRepository.save(unreadMelding)
        meldingNotificationRepository.save(readMelding)
        testEntityManager.flush()
        testEntityManager.clear()

        val unreadNotifications = notificationRepository.findByReadAndBehandlingIdAndNotMarkedAsDeleted(
            read = false,
            behandlingId = behandlingId
        )

        assertThat(unreadNotifications).hasSize(1)
        assertThat(unreadNotifications.first().id).isEqualTo(unreadMelding.id)
    }

    @Test
    fun `findByReadAndBehandlingIdAndNotMarkedAsDeleted finds unread LostAccessNotifications`() {
        val behandlingId = UUID.randomUUID()
        val now = LocalDateTime.now()

        // Create an unread LostAccessNotification
        val unreadLostAccess = createLostAccessNotification(
            behandlingId = behandlingId,
            read = false,
            markedAsDeleted = false,
            updatedAt = now
        )

        // Create a read LostAccessNotification (should NOT be found)
        val readLostAccess = createLostAccessNotification(
            behandlingId = behandlingId,
            read = true,
            markedAsDeleted = false,
            updatedAt = now
        )

        lostAccessNotificationRepository.save(unreadLostAccess)
        lostAccessNotificationRepository.save(readLostAccess)
        testEntityManager.flush()
        testEntityManager.clear()

        val unreadNotifications = notificationRepository.findByReadAndBehandlingIdAndNotMarkedAsDeleted(
            read = false,
            behandlingId = behandlingId
        )

        assertThat(unreadNotifications).hasSize(1)
        assertThat(unreadNotifications.first().id).isEqualTo(unreadLostAccess.id)
    }

    @Test
    fun `findByReadAndBehandlingIdAndNotMarkedAsDeleted finds both types of unread notifications`() {
        val behandlingId = UUID.randomUUID()
        val now = LocalDateTime.now()

        // Create an unread MeldingNotification
        val unreadMelding = createMeldingNotification(
            behandlingId = behandlingId,
            read = false,
            markedAsDeleted = false,
            updatedAt = now
        )

        // Create an unread LostAccessNotification
        val unreadLostAccess = createLostAccessNotification(
            behandlingId = behandlingId,
            read = false,
            markedAsDeleted = false,
            updatedAt = now
        )

        meldingNotificationRepository.save(unreadMelding)
        lostAccessNotificationRepository.save(unreadLostAccess)
        testEntityManager.flush()
        testEntityManager.clear()

        val unreadNotifications = notificationRepository.findByReadAndBehandlingIdAndNotMarkedAsDeleted(
            read = false,
            behandlingId = behandlingId
        )

        assertThat(unreadNotifications).hasSize(2)
        assertThat(unreadNotifications).anyMatch { it is MeldingNotification }
        assertThat(unreadNotifications).anyMatch { it is LostAccessNotification }
    }

    @Test
    fun `findByReadAndBehandlingIdAndNotMarkedAsDeleted excludes marked as deleted notifications`() {
        val behandlingId = UUID.randomUUID()
        val now = LocalDateTime.now()

        // Create an unread notification marked as deleted (should NOT be found)
        val deletedUnreadMelding = createMeldingNotification(
            behandlingId = behandlingId,
            read = false,
            markedAsDeleted = true,
            updatedAt = now
        )

        // Create an unread notification not marked as deleted (should be found)
        val unreadMelding = createMeldingNotification(
            behandlingId = behandlingId,
            read = false,
            markedAsDeleted = false,
            updatedAt = now
        )

        meldingNotificationRepository.save(deletedUnreadMelding)
        meldingNotificationRepository.save(unreadMelding)
        testEntityManager.flush()
        testEntityManager.clear()

        val unreadNotifications = notificationRepository.findByReadAndBehandlingIdAndNotMarkedAsDeleted(
            read = false,
            behandlingId = behandlingId
        )

        assertThat(unreadNotifications).hasSize(1)
        assertThat(unreadNotifications.first().id).isEqualTo(unreadMelding.id)
    }

    @Test
    fun `findByReadAndBehandlingIdAndNotMarkedAsDeleted returns empty list when no unread notifications exist`() {
        val behandlingId = UUID.randomUUID()
        val now = LocalDateTime.now()

        // Create only read notifications
        val readMelding = createMeldingNotification(
            behandlingId = behandlingId,
            read = true,
            markedAsDeleted = false,
            updatedAt = now
        )

        meldingNotificationRepository.save(readMelding)
        testEntityManager.flush()
        testEntityManager.clear()

        val unreadNotifications = notificationRepository.findByReadAndBehandlingIdAndNotMarkedAsDeleted(
            read = false,
            behandlingId = behandlingId
        )

        assertThat(unreadNotifications).isEmpty()
    }

    @Test
    fun `findByReadAndBehandlingIdAndNotMarkedAsDeleted only returns notifications for specified behandlingId`() {
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        val now = LocalDateTime.now()

        // Create unread notification for behandlingId1
        val unreadMelding1 = createMeldingNotification(
            behandlingId = behandlingId1,
            read = false,
            markedAsDeleted = false,
            updatedAt = now
        )

        // Create unread notification for behandlingId2 (should NOT be found)
        val unreadMelding2 = createMeldingNotification(
            behandlingId = behandlingId2,
            read = false,
            markedAsDeleted = false,
            updatedAt = now
        )

        meldingNotificationRepository.save(unreadMelding1)
        meldingNotificationRepository.save(unreadMelding2)
        testEntityManager.flush()
        testEntityManager.clear()

        val unreadNotifications = notificationRepository.findByReadAndBehandlingIdAndNotMarkedAsDeleted(
            read = false,
            behandlingId = behandlingId1
        )

        assertThat(unreadNotifications).hasSize(1)
        assertThat(unreadNotifications.first().id).isEqualTo(unreadMelding1.id)
    }

    private fun createMeldingNotification(
        behandlingId: UUID,
        read: Boolean,
        markedAsDeleted: Boolean,
        updatedAt: LocalDateTime
    ): MeldingNotification {
        return MeldingNotification(
            id = UUID.randomUUID(),
            message = "Test notification",
            navIdent = "Z123456",
            read = read,
            createdAt = updatedAt,
            updatedAt = updatedAt,
            readAt = if (read) updatedAt else null,
            markedAsDeleted = markedAsDeleted,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = updatedAt.minusDays(1),
            behandlingId = behandlingId,
            meldingId = UUID.randomUUID(),
            actorNavIdent = "Z999999",
            actorNavn = "Test Actor",
            saksnummer = "202312345",
            ytelse = Ytelse.OMS_OMP,
            behandlingType = Type.KLAGE
        )
    }

    private fun createLostAccessNotification(
        behandlingId: UUID,
        read: Boolean,
        markedAsDeleted: Boolean,
        updatedAt: LocalDateTime
    ): LostAccessNotification {
        return LostAccessNotification(
            id = UUID.randomUUID(),
            message = "Lost access notification",
            navIdent = "Z123456",
            read = read,
            createdAt = updatedAt,
            updatedAt = updatedAt,
            readAt = if (read) updatedAt else null,
            markedAsDeleted = markedAsDeleted,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = updatedAt.minusDays(1),
            behandlingId = behandlingId,
            saksnummer = "202312345",
            ytelse = Ytelse.SYK_SYK,
            behandlingType = Type.ANKE
        )
    }
}