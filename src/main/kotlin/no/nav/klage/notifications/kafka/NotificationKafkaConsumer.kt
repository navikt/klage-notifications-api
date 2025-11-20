package no.nav.klage.notifications.kafka

import jakarta.annotation.PostConstruct
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.getLogger
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.util.*

@Component
class NotificationKafkaConsumer(
    private val aivenKafkaClientCreator: AivenKafkaClientCreator,
    private val notificationService: NotificationService,
    private val environment: Environment,
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
                if (environment.activeProfiles.contains("dev-gcp")) {
                    logger.debug(
                        "Received Kafka-message (notification) from kabal-api at offset {}: {}",
                        record.offset(),
                        record.value(),
                    )
                } else {
                    logger.debug(
                        "Received Kafka-message (notification) from kabal-api at offset {}",
                        record.offset(),
                    )
                }
                try {
                    notificationService.processNotificationMessage(
                        kafkaMessageId = UUID.fromString(record.key()!!),
                        createNotificationEvent = record.value(),
                    )
                } catch (e: Exception) {
                    logger.error("Error processing notification message: ${e.message}", e)
                }
            }
    }

}