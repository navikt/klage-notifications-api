package no.nav.klage.notifications.repository

import no.nav.klage.kodeverk.Type
import no.nav.klage.notifications.db.PostgresIntegrationTestBase
import no.nav.klage.notifications.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.*


@ActiveProfiles("local")
@DataJpaTest
class NotificationRepositoryTest : PostgresIntegrationTestBase() {

    @Autowired
    lateinit var testEntityManager: TestEntityManager

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
            title = "New message received",
            message = "You have a new message in case $behandlingId",
            navIdent = navIdent,
            severity = NotificationSeverity.LOW,
            status = NotificationStatus.UNREAD,
            source = NotificationSource.KABAL,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            behandlingId = behandlingId,
            meldingId = meldingId,
            senderNavIdent = "Z999999",
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
        assertThat(found.get().senderNavIdent).isEqualTo("Z999999")
        assertThat(found.get().behandlingType).isEqualTo(Type.KLAGE)
        assertThat(found.get().navIdent).isEqualTo(navIdent)
        assertThat(found.get().title).isEqualTo("New message received")
    }

    @Test
    fun `persist lostAccessNotification works`() {
        val behandlingId = UUID.randomUUID()
        val navIdent = "Z123456"
        val now = LocalDateTime.now()

        val lostAccessNotification = LostAccessNotification(
            id = UUID.randomUUID(),
            title = "Lost access to case",
            message = "You no longer have access to case $behandlingId",
            navIdent = navIdent,
            severity = NotificationSeverity.MEDIUM,
            status = NotificationStatus.UNREAD,
            source = NotificationSource.KABAL,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            behandlingId = behandlingId,
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
        assertThat(found.get().title).isEqualTo("Lost access to case")
    }

}