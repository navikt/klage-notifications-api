package no.nav.klage.notifications.service

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.domain.LostAccessNotification
import no.nav.klage.notifications.domain.MeldingNotification
import no.nav.klage.notifications.domain.NotificationSource
import no.nav.klage.notifications.domain.SystemNotification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class NotificationMetricsServiceTest {

    private lateinit var meterRegistry: MeterRegistry
    private lateinit var metricsService: NotificationMetricsService

    @BeforeEach
    fun setup() {
        meterRegistry = SimpleMeterRegistry()
        metricsService = NotificationMetricsService(meterRegistry)
    }

    @Test
    fun `test record notification created increments counter`() {
        // Given
        val notification = createMeldingNotification()

        // When
        metricsService.recordNotificationCreated(notification)

        // Then
        val counter = meterRegistry.find("klage_notifications_created_total")
            .tag("notification_type", "MELDING")
            .tag("source", "KABAL")
            .counter()

        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    @Test
    fun `test record notification read increments counter and records time`() {
        // Given
        val notification = createMeldingNotification()
        notification.readAt = LocalDateTime.now()

        // When
        metricsService.recordNotificationRead(notification)

        // Then
        val readCounter = meterRegistry.find("klage_notifications_read_total")
            .tag("notification_type", "MELDING")
            .tag("source", "KABAL")
            .counter()

        assertNotNull(readCounter)
        assertEquals(1.0, readCounter!!.count())

        val timer = meterRegistry.find("klage_notifications_time_to_read_seconds")
            .tag("notification_type", "MELDING")
            .tag("source", "KABAL")
            .timer()

        assertNotNull(timer)
        assertEquals(1L, timer!!.count())
    }

    @Test
    fun `test record notification unread increments counter`() {
        // Given
        val notification = createMeldingNotification()

        // When
        metricsService.recordNotificationUnread(notification)

        // Then
        val counter = meterRegistry.find("klage_notifications_unread_total")
            .tag("notification_type", "MELDING")
            .tag("source", "KABAL")
            .counter()

        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    @Test
    fun `test record notification deleted increments counter`() {
        // Given
        val notification = createMeldingNotification()

        // When
        metricsService.recordNotificationDeleted(notification)

        // Then
        val counter = meterRegistry.find("klage_notifications_deleted_total")
            .tag("notification_type", "MELDING")
            .tag("source", "KABAL")
            .counter()

        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    @Test
    fun `test record system notification created increments counter`() {
        // Given
        val notification = createSystemNotification()

        // When
        metricsService.recordSystemNotificationCreated(notification)

        // Then
        val counter = meterRegistry.find("klage_notifications_created_total")
            .tag("notification_type", "SYSTEM")
            .tag("source", "KABAL")
            .counter()

        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    @Test
    fun `test record multiple notifications read`() {
        // Given
        val notifications = listOf(
            createMeldingNotification(),
            createMeldingNotification(),
            createLostAccessNotification()
        )
        notifications.forEach { it.readAt = LocalDateTime.now() }

        // When
        metricsService.recordMultipleNotificationsRead(notifications)

        // Then
        val meldingCounter = meterRegistry.find("klage_notifications_read_total")
            .tag("notification_type", "MELDING")
            .tag("source", "KABAL")
            .counter()

        val lostAccessCounter = meterRegistry.find("klage_notifications_read_total")
            .tag("notification_type", "LOST_ACCESS")
            .tag("source", "KABAL")
            .counter()

        assertNotNull(meldingCounter)
        assertNotNull(lostAccessCounter)
        assertEquals(2.0, meldingCounter!!.count())
        assertEquals(1.0, lostAccessCounter!!.count())
    }

    @Test
    fun `test different notification types are tracked separately`() {
        // Given
        val meldingNotification = createMeldingNotification()
        val lostAccessNotification = createLostAccessNotification()

        // When
        metricsService.recordNotificationCreated(meldingNotification)
        metricsService.recordNotificationCreated(lostAccessNotification)

        // Then
        val meldingCounter = meterRegistry.find("klage_notifications_created_total")
            .tag("notification_type", "MELDING")
            .counter()

        val lostAccessCounter = meterRegistry.find("klage_notifications_created_total")
            .tag("notification_type", "LOST_ACCESS")
            .counter()

        assertNotNull(meldingCounter)
        assertNotNull(lostAccessCounter)
        assertEquals(1.0, meldingCounter!!.count())
        assertEquals(1.0, lostAccessCounter!!.count())
    }

    @Test
    fun `test different sources are tracked separately`() {
        // Given
        val notification1 = createMeldingNotification(source = NotificationSource.KABAL)
        val notification2 = createMeldingNotification(source = NotificationSource.OPPGAVE)

        // When
        metricsService.recordNotificationCreated(notification1)
        metricsService.recordNotificationCreated(notification2)

        // Then
        val kabalCounter = meterRegistry.find("klage_notifications_created_total")
            .tag("source", "KABAL")
            .counter()

        val oppgaveCounter = meterRegistry.find("klage_notifications_created_total")
            .tag("source", "OPPGAVE")
            .counter()

        assertNotNull(kabalCounter)
        assertNotNull(oppgaveCounter)
        assertEquals(1.0, kabalCounter!!.count())
        assertEquals(1.0, oppgaveCounter!!.count())
    }

    // Helper methods to create test notifications

    private fun createMeldingNotification(source: NotificationSource = NotificationSource.KABAL): MeldingNotification {
        val now = LocalDateTime.now()
        return MeldingNotification(
            id = UUID.randomUUID(),
            message = "Test message",
            navIdent = "Z999999",
            read = false,
            source = source,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = now,
            behandlingId = UUID.randomUUID(),
            meldingId = UUID.randomUUID(),
            actorNavIdent = "Z888888",
            actorNavn = "Test Actor",
            saksnummer = "12345",
            ytelse = Ytelse.OMS_OMP,
            behandlingType = Type.KLAGE,
        )
    }

    private fun createLostAccessNotification(source: NotificationSource = NotificationSource.KABAL): LostAccessNotification {
        val now = LocalDateTime.now()
        return LostAccessNotification(
            id = UUID.randomUUID(),
            message = "Lost access message",
            navIdent = "Z999999",
            read = false,
            source = source,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = now,
            behandlingId = UUID.randomUUID(),
            saksnummer = "12345",
            ytelse = Ytelse.OMS_OMP,
            behandlingType = Type.KLAGE,
        )
    }

    private fun createSystemNotification(source: NotificationSource = NotificationSource.KABAL): SystemNotification {
        val now = LocalDateTime.now()
        return SystemNotification(
            id = UUID.randomUUID(),
            title = "System notification",
            message = "System message",
            source = source,
            createdAt = now,
            updatedAt = now,
            markedAsDeleted = false,
        )
    }
}