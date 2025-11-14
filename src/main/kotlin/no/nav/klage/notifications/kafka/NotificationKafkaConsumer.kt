package no.nav.klage.notifications.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.getLogger
import org.springframework.stereotype.Component
import java.util.*

@Component
class NotificationKafkaConsumer(
    private val aivenKafkaClientCreator: AivenKafkaClientCreator,
    private val notificationService: NotificationService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @PostConstruct
    fun startConsuming() {
        logger.info("Starting Kafka consumer for persisting notification events")
        val receiver = aivenKafkaClientCreator.getNewKafkaNotificationEventsReceiver()
        receiver.receive()
            .subscribe { record ->
                try {
                    notificationService.processNotificationMessage(
                        messageId = UUID.fromString(record.key()!!),
                        jsonNode = jacksonObjectMapper().readTree(record.value()),
                    )
                } catch (e: Exception) {
                    logger.error("Error processing notification message: ${e.message}", e)
                }
            }
    }

}