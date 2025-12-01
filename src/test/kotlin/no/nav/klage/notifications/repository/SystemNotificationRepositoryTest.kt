package no.nav.klage.notifications.repository

import no.nav.klage.notifications.db.PostgresIntegrationTestBase
import no.nav.klage.notifications.domain.SystemNotification
import no.nav.klage.notifications.domain.SystemNotificationReadStatus
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
class SystemNotificationRepositoryTest : PostgresIntegrationTestBase() {

    @Autowired
    lateinit var testEntityManager: TestEntityManager

    @Autowired
    lateinit var systemNotificationRepository: SystemNotificationRepository

    @Autowired
    lateinit var systemNotificationReadStatusRepository: SystemNotificationReadStatusRepository

    @BeforeEach
    fun setup() {
        systemNotificationReadStatusRepository.deleteAll()
        systemNotificationRepository.deleteAll()
    }

    @Test
    fun `save and find system notification`() {
        val now = LocalDateTime.now()
        val notification = SystemNotification(
            id = UUID.randomUUID(),
            title = "System Maintenance",
            message = "The system will be down for maintenance on Saturday",
            createdAt = now,
            updatedAt = now,
            markedAsDeleted = false
        )

        val saved = systemNotificationRepository.save(notification)
        testEntityManager.flush()
        testEntityManager.clear()

        val found = systemNotificationRepository.findById(saved.id)
        assertThat(found).isPresent
        assertThat(found.get()).isEqualTo(saved)
        assertThat(found.get().title).isEqualTo("System Maintenance")
        assertThat(found.get().message).isEqualTo("The system will be down for maintenance on Saturday")
        assertThat(found.get().markedAsDeleted).isFalse()
    }

    @Test
    fun `findByMarkedAsDeletedOrderByCreatedAtDesc returns only non-deleted notifications`() {
        val now = LocalDateTime.now()

        // Create active notification
        val activeNotification = systemNotificationRepository.save(
            SystemNotification(
                title = "Active Notification",
                message = "This is active",
                createdAt = now,
                updatedAt = now,
                markedAsDeleted = false
            )
        )

        // Create deleted notification
        systemNotificationRepository.save(
            SystemNotification(
                title = "Deleted Notification",
                message = "This is deleted",
                createdAt = now.minusDays(1),
                updatedAt = now,
                markedAsDeleted = true
            )
        )

        testEntityManager.flush()

        val activeNotifications = systemNotificationRepository.findByMarkedAsDeletedOrderByCreatedAtDesc(false)

        assertThat(activeNotifications).hasSize(1)
        assertThat(activeNotifications[0].id).isEqualTo(activeNotification.id)
        assertThat(activeNotifications[0].title).isEqualTo("Active Notification")
    }

    @Test
    fun `save and find system notification read status`() {
        val now = LocalDateTime.now()

        // Create a system notification first
        val notification = systemNotificationRepository.save(
            SystemNotification(
                title = "Test Notification",
                message = "Test message",
                createdAt = now,
                updatedAt = now,
                markedAsDeleted = false
            )
        )
        testEntityManager.flush()

        // Create read status
        val readStatus = SystemNotificationReadStatus(
            id = UUID.randomUUID(),
            systemNotificationId = notification.id,
            navIdent = "Z123456",
            readAt = now
        )

        val saved = systemNotificationReadStatusRepository.save(readStatus)
        testEntityManager.flush()
        testEntityManager.clear()

        val found = systemNotificationReadStatusRepository.findById(saved.id)
        assertThat(found).isPresent
        assertThat(found.get()).isEqualTo(saved)
        assertThat(found.get().systemNotificationId).isEqualTo(notification.id)
        assertThat(found.get().navIdent).isEqualTo("Z123456")
    }

    @Test
    fun `existsBySystemNotificationIdAndNavIdent returns true when exists`() {
        val now = LocalDateTime.now()
        val notification = createAndSaveSystemNotification(now)

        val readStatus = SystemNotificationReadStatus(
            systemNotificationId = notification.id,
            navIdent = "Z123456",
            readAt = now
        )
        systemNotificationReadStatusRepository.save(readStatus)
        testEntityManager.flush()

        val exists = systemNotificationReadStatusRepository.existsBySystemNotificationIdAndNavIdent(
            notification.id,
            "Z123456"
        )

        assertThat(exists).isTrue()
    }

    @Test
    fun `existsBySystemNotificationIdAndNavIdent returns false when does not exist`() {
        val now = LocalDateTime.now()
        val notification = createAndSaveSystemNotification(now)

        val exists = systemNotificationReadStatusRepository.existsBySystemNotificationIdAndNavIdent(
            notification.id,
            "Z999999"
        )

        assertThat(exists).isFalse()
    }

    @Test
    fun `findBySystemNotificationIdAndNavIdent returns status when exists`() {
        val now = LocalDateTime.now()
        val notification = createAndSaveSystemNotification(now)

        val readStatus = SystemNotificationReadStatus(
            systemNotificationId = notification.id,
            navIdent = "Z123456",
            readAt = now
        )
        systemNotificationReadStatusRepository.save(readStatus)
        testEntityManager.flush()

        val found = systemNotificationReadStatusRepository.findBySystemNotificationIdAndNavIdent(
            notification.id,
            "Z123456"
        )

        assertThat(found).isNotNull
        assertThat(found?.navIdent).isEqualTo("Z123456")
        assertThat(found?.systemNotificationId).isEqualTo(notification.id)
    }

    @Test
    fun `findBySystemNotificationIdAndNavIdent returns null when does not exist`() {
        val now = LocalDateTime.now()
        val notification = createAndSaveSystemNotification(now)

        val found = systemNotificationReadStatusRepository.findBySystemNotificationIdAndNavIdent(
            notification.id,
            "Z999999"
        )

        assertThat(found).isNull()
    }

    @Test
    fun `deleteBySystemNotificationIdAndNavIdent removes the read status`() {
        val now = LocalDateTime.now()
        val notification = createAndSaveSystemNotification(now)

        val readStatus = SystemNotificationReadStatus(
            systemNotificationId = notification.id,
            navIdent = "Z123456",
            readAt = now
        )
        systemNotificationReadStatusRepository.save(readStatus)
        testEntityManager.flush()

        // Verify it exists
        assertThat(
            systemNotificationReadStatusRepository.existsBySystemNotificationIdAndNavIdent(
                notification.id,
                "Z123456"
            )
        ).isTrue()

        // Delete it
        systemNotificationReadStatusRepository.deleteBySystemNotificationIdAndNavIdent(
            notification.id,
            "Z123456"
        )
        testEntityManager.flush()

        // Verify it's gone
        assertThat(
            systemNotificationReadStatusRepository.existsBySystemNotificationIdAndNavIdent(
                notification.id,
                "Z123456"
            )
        ).isFalse()
    }

    @Test
    fun `multiple users can have different read status for same system notification`() {
        val now = LocalDateTime.now()
        val notification = createAndSaveSystemNotification(now)

        // User 1 has read it
        systemNotificationReadStatusRepository.save(
            SystemNotificationReadStatus(
                systemNotificationId = notification.id,
                navIdent = "Z111111",
                readAt = now
            )
        )

        // User 2 has read it
        systemNotificationReadStatusRepository.save(
            SystemNotificationReadStatus(
                systemNotificationId = notification.id,
                navIdent = "Z222222",
                readAt = now.plusHours(1)
            )
        )

        testEntityManager.flush()

        // Verify both exist
        val user1Exists = systemNotificationReadStatusRepository.existsBySystemNotificationIdAndNavIdent(
            notification.id,
            "Z111111"
        )
        val user2Exists = systemNotificationReadStatusRepository.existsBySystemNotificationIdAndNavIdent(
            notification.id,
            "Z222222"
        )
        val user3Exists = systemNotificationReadStatusRepository.existsBySystemNotificationIdAndNavIdent(
            notification.id,
            "Z333333"
        )

        assertThat(user1Exists).isTrue()
        assertThat(user2Exists).isTrue()
        assertThat(user3Exists).isFalse()
    }

    @Test
    fun `unique constraint prevents duplicate read status for same user and notification`() {
        val now = LocalDateTime.now()
        val notification = createAndSaveSystemNotification(now)

        val readStatus1 = SystemNotificationReadStatus(
            systemNotificationId = notification.id,
            navIdent = "Z123456",
            readAt = now
        )
        systemNotificationReadStatusRepository.save(readStatus1)
        testEntityManager.flush()

        // Try to create duplicate
        val readStatus2 = SystemNotificationReadStatus(
            systemNotificationId = notification.id,
            navIdent = "Z123456",
            readAt = now.plusHours(1)
        )

        try {
            systemNotificationReadStatusRepository.save(readStatus2)
            testEntityManager.flush()
            assertThat(false).describedAs("Should have thrown constraint violation").isTrue()
        } catch (e: Exception) {
            // Expected - unique constraint violation
            assertThat(e).isNotNull
        }
    }

    @Test
    fun `cascade delete removes read status when system notification is deleted`() {
        val now = LocalDateTime.now()
        val notification = createAndSaveSystemNotification(now)

        // Create read statuses for multiple users
        systemNotificationReadStatusRepository.save(
            SystemNotificationReadStatus(
                systemNotificationId = notification.id,
                navIdent = "Z111111",
                readAt = now
            )
        )
        systemNotificationReadStatusRepository.save(
            SystemNotificationReadStatus(
                systemNotificationId = notification.id,
                navIdent = "Z222222",
                readAt = now
            )
        )
        testEntityManager.flush()

        // Verify read statuses exist
        assertThat(systemNotificationReadStatusRepository.count()).isEqualTo(2)

        // Delete the system notification
        systemNotificationRepository.delete(notification)
        testEntityManager.flush()
        testEntityManager.clear()

        // Verify read statuses are also deleted (cascade)
        assertThat(systemNotificationReadStatusRepository.count()).isEqualTo(0)
    }

    private fun createAndSaveSystemNotification(
        now: LocalDateTime,
        title: String = "Test Title",
        message: String = "Test message"
    ): SystemNotification {
        val notification = SystemNotification(
            title = title,
            message = message,
            createdAt = now,
            updatedAt = now,
            markedAsDeleted = false
        )
        return systemNotificationRepository.save(notification)
    }
}