package no.nav.klage.notifications.service

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.klage.kodeverk.Type
import no.nav.klage.kodeverk.ytelse.Ytelse
import no.nav.klage.notifications.domain.LostAccessNotification
import no.nav.klage.notifications.domain.MeldingNotification
import no.nav.klage.notifications.domain.NotificationType
import no.nav.klage.notifications.repository.NotificationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class NotificationAggregateMetricsServiceTest {

    private lateinit var meterRegistry: PrometheusMeterRegistry
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var service: NotificationAggregateMetricsService

    @BeforeEach
    fun setup() {
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        notificationRepository = mockk()
        service = NotificationAggregateMetricsService(notificationRepository, meterRegistry)
    }

    @Test
    fun `gauges are initialized with default values on startup`() {
        // When
        service.initializeGauges()

        // Then - Verify all gauges exist for each notification type
        NotificationType.entries.forEach { type ->
            // Behandling gauges
            val minBehandlingGauge = meterRegistry.find("klage_notifications_per_behandling_min_gauge")
                .tag("notification_type", type.name)
                .gauge()
            assertThat(minBehandlingGauge).isNotNull
            assertThat(minBehandlingGauge!!.value()).isEqualTo(0.0)

            val maxBehandlingGauge = meterRegistry.find("klage_notifications_per_behandling_max_gauge")
                .tag("notification_type", type.name)
                .gauge()
            assertThat(maxBehandlingGauge).isNotNull
            assertThat(maxBehandlingGauge!!.value()).isEqualTo(0.0)

            val avgBehandlingGauge = meterRegistry.find("klage_notifications_per_behandling_avg_gauge")
                .tag("notification_type", type.name)
                .gauge()
            assertThat(avgBehandlingGauge).isNotNull
            assertThat(avgBehandlingGauge!!.value()).isEqualTo(0.0)

            // User gauges
            val minUserGauge = meterRegistry.find("klage_notifications_per_user_min_gauge")
                .tag("notification_type", type.name)
                .gauge()
            assertThat(minUserGauge).isNotNull
            assertThat(minUserGauge!!.value()).isEqualTo(0.0)

            val maxUserGauge = meterRegistry.find("klage_notifications_per_user_max_gauge")
                .tag("notification_type", type.name)
                .gauge()
            assertThat(maxUserGauge).isNotNull
            assertThat(maxUserGauge!!.value()).isEqualTo(0.0)

            val avgUserGauge = meterRegistry.find("klage_notifications_per_user_avg_gauge")
                .tag("notification_type", type.name)
                .gauge()
            assertThat(avgUserGauge).isNotNull
            assertThat(avgUserGauge!!.value()).isEqualTo(0.0)

            // Unread count gauge
            val unreadGauge = meterRegistry.find("klage_notifications_unread_total_gauge")
                .tag("notification_type", type.name)
                .gauge()
            assertThat(unreadGauge).isNotNull
            assertThat(unreadGauge!!.value()).isEqualTo(0.0)
        }
    }

    @Test
    fun `updateAggregateMetrics updates behandling metrics correctly for MELDING notifications`() {
        // Given
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        val notifications = listOf(
            createMeldingNotification(behandlingId = behandlingId1, navIdent = "Z111111"),
            createMeldingNotification(behandlingId = behandlingId1, navIdent = "Z111111"),
            createMeldingNotification(behandlingId = behandlingId1, navIdent = "Z111111"),
            createMeldingNotification(behandlingId = behandlingId2, navIdent = "Z222222"),
        )

        every { notificationRepository.findAll() } returns notifications
        service.initializeGauges()

        // When
        service.updateAggregateMetrics()

        // Then - behandling1 has 3 notifications, behandling2 has 1
        val minGauge = meterRegistry.find("klage_notifications_per_behandling_min_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(minGauge!!.value()).isEqualTo(1.0)

        val maxGauge = meterRegistry.find("klage_notifications_per_behandling_max_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(maxGauge!!.value()).isEqualTo(3.0)

        val avgGauge = meterRegistry.find("klage_notifications_per_behandling_avg_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(avgGauge!!.value()).isEqualTo(2.0) // (3 + 1) / 2 = 2.0
    }

    @Test
    fun `updateAggregateMetrics updates user metrics correctly for MELDING notifications`() {
        // Given
        val notifications = listOf(
            createMeldingNotification(navIdent = "Z111111"),
            createMeldingNotification(navIdent = "Z111111"),
            createMeldingNotification(navIdent = "Z111111"),
            createMeldingNotification(navIdent = "Z111111"),
            createMeldingNotification(navIdent = "Z222222"),
            createMeldingNotification(navIdent = "Z222222"),
        )

        every { notificationRepository.findAll() } returns notifications
        service.initializeGauges()

        // When
        service.updateAggregateMetrics()

        // Then - user1 has 4 notifications, user2 has 2
        val minGauge = meterRegistry.find("klage_notifications_per_user_min_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(minGauge!!.value()).isEqualTo(2.0)

        val maxGauge = meterRegistry.find("klage_notifications_per_user_max_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(maxGauge!!.value()).isEqualTo(4.0)

        val avgGauge = meterRegistry.find("klage_notifications_per_user_avg_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(avgGauge!!.value()).isEqualTo(3.0) // (4 + 2) / 2 = 3.0
    }

    @Test
    fun `updateAggregateMetrics updates behandling metrics correctly for LOST_ACCESS notifications`() {
        // Given
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        val notifications = listOf(
            createLostAccessNotification(behandlingId = behandlingId1),
            createLostAccessNotification(behandlingId = behandlingId2),
            createLostAccessNotification(behandlingId = behandlingId2),
        )

        every { notificationRepository.findAll() } returns notifications
        service.initializeGauges()

        // When
        service.updateAggregateMetrics()

        // Then - behandling1 has 1 notification, behandling2 has 2
        val minGauge = meterRegistry.find("klage_notifications_per_behandling_min_gauge")
            .tag("notification_type", "LOST_ACCESS")
            .gauge()
        assertThat(minGauge!!.value()).isEqualTo(1.0)

        val maxGauge = meterRegistry.find("klage_notifications_per_behandling_max_gauge")
            .tag("notification_type", "LOST_ACCESS")
            .gauge()
        assertThat(maxGauge!!.value()).isEqualTo(2.0)

        val avgGauge = meterRegistry.find("klage_notifications_per_behandling_avg_gauge")
            .tag("notification_type", "LOST_ACCESS")
            .gauge()
        assertThat(avgGauge!!.value()).isEqualTo(1.5) // (1 + 2) / 2 = 1.5
    }

    @Test
    fun `updateAggregateMetrics updates unread count correctly`() {
        // Given
        val notifications = listOf(
            createMeldingNotification(read = false),
            createMeldingNotification(read = false),
            createMeldingNotification(read = true),
            createLostAccessNotification(read = false),
            createLostAccessNotification(read = true),
        )

        every { notificationRepository.findAll() } returns notifications
        service.initializeGauges()

        // When
        service.updateAggregateMetrics()

        // Then
        val meldingUnreadGauge = meterRegistry.find("klage_notifications_unread_total_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(meldingUnreadGauge!!.value()).isEqualTo(2.0)

        val lostAccessUnreadGauge = meterRegistry.find("klage_notifications_unread_total_gauge")
            .tag("notification_type", "LOST_ACCESS")
            .gauge()
        assertThat(lostAccessUnreadGauge!!.value()).isEqualTo(1.0)
    }

    @Test
    fun `updateAggregateMetrics excludes marked as deleted notifications from behandling metrics`() {
        // Given
        val behandlingId = UUID.randomUUID()
        val notifications = listOf(
            createMeldingNotification(behandlingId = behandlingId, markedAsDeleted = false),
            createMeldingNotification(behandlingId = behandlingId, markedAsDeleted = false),
            createMeldingNotification(behandlingId = behandlingId, markedAsDeleted = true),
        )

        every { notificationRepository.findAll() } returns notifications
        service.initializeGauges()

        // When
        service.updateAggregateMetrics()

        // Then - Only 2 active notifications should be counted
        val minGauge = meterRegistry.find("klage_notifications_per_behandling_min_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(minGauge!!.value()).isEqualTo(2.0)

        val maxGauge = meterRegistry.find("klage_notifications_per_behandling_max_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(maxGauge!!.value()).isEqualTo(2.0)
    }

    @Test
    fun `updateAggregateMetrics excludes marked as deleted notifications from user metrics`() {
        // Given
        val navIdent = "Z111111"
        val notifications = listOf(
            createMeldingNotification(navIdent = navIdent, markedAsDeleted = false),
            createMeldingNotification(navIdent = navIdent, markedAsDeleted = false),
            createMeldingNotification(navIdent = navIdent, markedAsDeleted = true),
        )

        every { notificationRepository.findAll() } returns notifications
        service.initializeGauges()

        // When
        service.updateAggregateMetrics()

        // Then - Only 2 active notifications should be counted
        val minGauge = meterRegistry.find("klage_notifications_per_user_min_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(minGauge!!.value()).isEqualTo(2.0)

        val maxGauge = meterRegistry.find("klage_notifications_per_user_max_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(maxGauge!!.value()).isEqualTo(2.0)
    }

    @Test
    fun `updateAggregateMetrics excludes marked as deleted notifications from unread count`() {
        // Given
        val notifications = listOf(
            createMeldingNotification(read = false, markedAsDeleted = false),
            createMeldingNotification(read = false, markedAsDeleted = false),
            createMeldingNotification(read = false, markedAsDeleted = true),
        )

        every { notificationRepository.findAll() } returns notifications
        service.initializeGauges()

        // When
        service.updateAggregateMetrics()

        // Then - Only 2 unread active notifications should be counted
        val unreadGauge = meterRegistry.find("klage_notifications_unread_total_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(unreadGauge!!.value()).isEqualTo(2.0)
    }

    @Test
    fun `updateAggregateMetrics handles empty notifications list`() {
        // Given
        every { notificationRepository.findAll() } returns emptyList()
        service.initializeGauges()

        // When
        service.updateAggregateMetrics()

        // Then - All gauges should remain at 0
        NotificationType.entries.forEach { type ->
            val minBehandlingGauge = meterRegistry.find("klage_notifications_per_behandling_min_gauge")
                .tag("notification_type", type.name)
                .gauge()
            assertThat(minBehandlingGauge!!.value()).isEqualTo(0.0)

            val maxBehandlingGauge = meterRegistry.find("klage_notifications_per_behandling_max_gauge")
                .tag("notification_type", type.name)
                .gauge()
            assertThat(maxBehandlingGauge!!.value()).isEqualTo(0.0)

            val avgBehandlingGauge = meterRegistry.find("klage_notifications_per_behandling_avg_gauge")
                .tag("notification_type", type.name)
                .gauge()
            assertThat(avgBehandlingGauge!!.value()).isEqualTo(0.0)

            val unreadGauge = meterRegistry.find("klage_notifications_unread_total_gauge")
                .tag("notification_type", type.name)
                .gauge()
            assertThat(unreadGauge!!.value()).isEqualTo(0.0)
        }
    }

    @Test
    fun `updateAggregateMetrics handles mixed notification types`() {
        // Given
        val behandlingId = UUID.randomUUID()
        val navIdent = "Z111111"
        val notifications = listOf(
            createMeldingNotification(behandlingId = behandlingId, navIdent = navIdent, read = false),
            createMeldingNotification(behandlingId = behandlingId, navIdent = navIdent, read = false),
            createLostAccessNotification(behandlingId = behandlingId, navIdent = navIdent, read = true),
        )

        every { notificationRepository.findAll() } returns notifications
        service.initializeGauges()

        // When
        service.updateAggregateMetrics()

        // Then - Each type should be tracked separately
        val meldingUnreadGauge = meterRegistry.find("klage_notifications_unread_total_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(meldingUnreadGauge!!.value()).isEqualTo(2.0)

        val lostAccessUnreadGauge = meterRegistry.find("klage_notifications_unread_total_gauge")
            .tag("notification_type", "LOST_ACCESS")
            .gauge()
        assertThat(lostAccessUnreadGauge!!.value()).isEqualTo(0.0)

        val meldingPerBehandlingGauge = meterRegistry.find("klage_notifications_per_behandling_min_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(meldingPerBehandlingGauge!!.value()).isEqualTo(2.0)

        val lostAccessPerBehandlingGauge = meterRegistry.find("klage_notifications_per_behandling_min_gauge")
            .tag("notification_type", "LOST_ACCESS")
            .gauge()
        assertThat(lostAccessPerBehandlingGauge!!.value()).isEqualTo(1.0)
    }

    @Test
    fun `updateAggregateMetrics does not fail when repository throws exception`() {
        // Given
        every { notificationRepository.findAll() } throws RuntimeException("Database error")
        service.initializeGauges()

        // When - Should not throw exception
        service.updateAggregateMetrics()

        // Then - Gauges should still have their initial values
        val minGauge = meterRegistry.find("klage_notifications_per_behandling_min_gauge")
            .tag("notification_type", "MELDING")
            .gauge()
        assertThat(minGauge!!.value()).isEqualTo(0.0)
    }

    @Test
    fun `prometheus output contains all gauge metrics`() {
        val notifications = listOf(
            createMeldingNotification(read = false),
            createMeldingNotification(read = false),
            createLostAccessNotification(read = true),
        )

        every { notificationRepository.findAll() } returns notifications
        service.initializeGauges()

        service.updateAggregateMetrics()

        val prometheusOutput = meterRegistry.scrape()

        println("=== Prometheus Aggregate Metrics Output ===")
        println(prometheusOutput)
        println("=== End of Prometheus Output ===")

        // Verify gauge metrics appear in Prometheus format
        assertThat(prometheusOutput).contains("klage_notifications_per_behandling_min_gauge")
        assertThat(prometheusOutput).contains("klage_notifications_per_behandling_max_gauge")
        assertThat(prometheusOutput).contains("klage_notifications_per_behandling_avg_gauge")
        assertThat(prometheusOutput).contains("klage_notifications_per_user_min_gauge")
        assertThat(prometheusOutput).contains("klage_notifications_per_user_max_gauge")
        assertThat(prometheusOutput).contains("klage_notifications_per_user_avg_gauge")
        assertThat(prometheusOutput).contains("klage_notifications_unread_total_gauge")
        assertThat(prometheusOutput).contains("notification_type=\"MELDING\"")
        assertThat(prometheusOutput).contains("notification_type=\"LOST_ACCESS\"")
    }

    // Helper methods to create test notifications

    private fun createMeldingNotification(
        behandlingId: UUID = UUID.randomUUID(),
        navIdent: String = "Z999999",
        read: Boolean = false,
        markedAsDeleted: Boolean = false
    ): MeldingNotification {
        val now = LocalDateTime.now()
        return MeldingNotification(
            id = UUID.randomUUID(),
            message = "Test message",
            navIdent = navIdent,
            read = read,
            createdAt = now,
            updatedAt = now,
            readAt = if (read) now else null,
            markedAsDeleted = markedAsDeleted,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = now,
            behandlingId = behandlingId,
            meldingId = UUID.randomUUID(),
            actorNavIdent = "Z888888",
            actorNavn = "Test Actor",
            saksnummer = "12345",
            ytelse = Ytelse.OMS_OMP,
            behandlingType = Type.KLAGE,
        )
    }

    private fun createLostAccessNotification(
        behandlingId: UUID = UUID.randomUUID(),
        navIdent: String = "Z999999",
        read: Boolean = false,
        markedAsDeleted: Boolean = false
    ): LostAccessNotification {
        val now = LocalDateTime.now()
        return LostAccessNotification(
            id = UUID.randomUUID(),
            message = "Lost access message",
            navIdent = navIdent,
            read = read,
            createdAt = now,
            updatedAt = now,
            readAt = if (read) now else null,
            markedAsDeleted = markedAsDeleted,
            kafkaMessageId = UUID.randomUUID(),
            sourceCreatedAt = now,
            behandlingId = behandlingId,
            saksnummer = "12345",
            ytelse = Ytelse.OMS_OMP,
            behandlingType = Type.KLAGE,
        )
    }
}