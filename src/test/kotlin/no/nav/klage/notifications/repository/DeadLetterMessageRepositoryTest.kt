package no.nav.klage.notifications.repository

import no.nav.klage.notifications.db.PostgresIntegrationTestBase
import no.nav.klage.notifications.domain.DeadLetterMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles("local")
@DataJpaTest
class DeadLetterMessageRepositoryTest : PostgresIntegrationTestBase() {
    @Autowired
    lateinit var deadLetterMessageRepository: DeadLetterMessageRepository

    @Test
    fun `test save and find dead letter message`() {
        // Given
        val deadLetterMessage = DeadLetterMessage(
            topic = "test-topic",
            messageKey = "test-key",
            messageValue = """{"test": "value"}""",
            kafkaOffset = 123L,
            partition = 0,
            errorMessage = "Test error",
            stackTrace = "Test stack trace",
            attemptCount = 3,
            firstAttemptAt = LocalDateTime.now().minusMinutes(5),
            lastAttemptAt = LocalDateTime.now(),
            processed = false,
            createdAt = LocalDateTime.now(),
            processedAt = null,
            reprocess = false,
            reprocessedAt = null,
        )
        // When
        val saved = deadLetterMessageRepository.save(deadLetterMessage)
        val found = deadLetterMessageRepository.findById(saved.id)
        // Then
        assertThat(found).isPresent
        assertThat(found.get().topic).isEqualTo("test-topic")
        assertThat(found.get().messageKey).isEqualTo("test-key")
        assertThat(found.get().attemptCount).isEqualTo(3)
        assertThat(found.get().processed).isFalse()
    }

}