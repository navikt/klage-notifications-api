package no.nav.klage.notifications.repository

import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.db.PostgresIntegrationTestBase
import no.nav.klage.notifications.domain.LostAccessNotification
import no.nav.klage.notifications.domain.MeldingNotification
import no.nav.klage.notifications.domain.NotificationSource
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
            message = "You have a new message in case $behandlingId",
            navIdent = navIdent,
            read = false,
            source = NotificationSource.KABAL,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = UUID.randomUUID(),
            behandlingId = behandlingId,
            meldingId = meldingId,
            actorNavIdent = "Z999999",
            actorNavn = "Test Testesen",
            saksnummer = "202312345",
            ytelse = Ytelse.OMS_OMP,
            meldingCreated = now.minusDays(1),
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
            source = NotificationSource.KABAL,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = UUID.randomUUID(),
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

}