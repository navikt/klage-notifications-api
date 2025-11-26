package no.nav.klage.notifications.service

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.domain.LostAccessNotification
import no.nav.klage.notifications.domain.MeldingNotification
import no.nav.klage.notifications.domain.NotificationSource
import no.nav.klage.notifications.domain.SystemNotification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class NotificationMetricsServiceTest {

    private lateinit var meterRegistry: PrometheusMeterRegistry
    private lateinit var metricsService: NotificationMetricsService

    @BeforeEach
    fun setup() {
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        metricsService = NotificationMetricsService(meterRegistry)
    }

    @Test
    fun `test prometheus output format`() {
        // Given
        val notification = createMeldingNotification()

        // When
        metricsService.recordNotificationCreated(notification)
        metricsService.recordNotificationCreated(notification)
        metricsService.recordNotificationCreated(notification)

        // Then - Get the actual Prometheus scrape output
        val prometheusOutput = meterRegistry.scrape()

        println("=== Prometheus Output ===")
        println(prometheusOutput)
        println("=== End of Prometheus Output ===")

        // Verify the metric appears in Prometheus format
        // Note: PrometheusMeterRegistry strips "_created" from counter names ending in "_total"
        // So "klage_notifications_total" becomes "klage_notifications_total"
        assertThat(prometheusOutput).contains("klage_notifications_total")
        assertThat(prometheusOutput).contains("notification_type=\"MELDING\"")
        assertThat(prometheusOutput).contains("source=\"KABAL\"")
        assertThat(prometheusOutput).contains("3.0")
    }

    @Test
    fun `test record notification created increments counter`() {
        // Given
        val notification = createMeldingNotification()

        // When
        metricsService.recordNotificationCreated(notification)

        // Then
        val counter = meterRegistry.find("klage_notifications_total")
            .tag("notification_type", "MELDING")
            .tag("source", "KABAL")
            .counter()

        assertThat(counter).isNotNull
        assertThat(counter!!.count()).isEqualTo(1.0)
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

        assertThat(readCounter).isNotNull
        assertThat(readCounter!!.count()).isEqualTo(1.0)

        val timer = meterRegistry.find("klage_notifications_time_to_read_seconds")
            .tag("notification_type", "MELDING")
            .tag("source", "KABAL")
            .timer()

        assertThat(timer).isNotNull
        assertThat(timer!!.count()).isEqualTo(1L)
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

        assertThat(counter).isNotNull
        assertThat(counter!!.count()).isEqualTo(1.0)
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

        assertThat(counter).isNotNull
        assertThat(counter!!.count()).isEqualTo(1.0)
    }

    @Test
    fun `test record system notification created increments counter`() {
        // Given
        val notification = createSystemNotification()

        // When
        metricsService.recordSystemNotificationCreated(notification)

        // Then
        val counter = meterRegistry.find("klage_notifications_total")
            .tag("notification_type", "SYSTEM")
            .tag("source", "KABAL")
            .counter()

        assertThat(counter).isNotNull
        assertThat(counter!!.count()).isEqualTo(1.0)
    }

    @Test
    fun `test record multiple notifications read`() {
        // Given
        val notifications = listOf(
            createMeldingNotification(),
            createMeldingNotification(),
            createLostAccessNotification(),
        )
        notifications.forEach { it.readAt = LocalDateTime.now() }

        // When
        metricsService.recordMultipleNotificationsRead(notifications)

        // Then
        val meldingCounters = meterRegistry.find("klage_notifications_read_total")
            .tag("notification_type", "MELDING")
            .tag("source", "KABAL")
            .counters()

        val lostAccessCounters = meterRegistry.find("klage_notifications_read_total")
            .tag("notification_type", "LOST_ACCESS")
            .tag("source", "KABAL")
            .counters()

        assertThat(meldingCounters).isNotNull
        assertThat(lostAccessCounters).isNotNull
        assertThat(meldingCounters.sumOf { it.count() }).isEqualTo(2.0)
        assertThat(lostAccessCounters.sumOf { it.count() }).isEqualTo(1.0)
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
        val meldingCounters = meterRegistry.find("klage_notifications_total")
            .tag("notification_type", "MELDING")
            .counters()

        val lostAccessCounters = meterRegistry.find("klage_notifications_total")
            .tag("notification_type", "LOST_ACCESS")
            .counters()

        assertThat(meldingCounters).isNotNull
        assertThat(lostAccessCounters).isNotNull
        assertThat(meldingCounters.sumOf { it.count() }).isEqualTo(1.0)
        assertThat(lostAccessCounters.sumOf { it.count() }).isEqualTo(1.0)
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
        val kabalCounters = meterRegistry.find("klage_notifications_total")
            .tag("source", "KABAL")
            .counters()

        val oppgaveCounters = meterRegistry.find("klage_notifications_total")
            .tag("source", "OPPGAVE")
            .counters()

        assertThat(kabalCounters).isNotNull
        assertThat(oppgaveCounters).isNotNull
        assertThat(kabalCounters.sumOf { it.count() }).isEqualTo(1.0)
        assertThat(oppgaveCounters.sumOf { it.count() }).isEqualTo(1.0)
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