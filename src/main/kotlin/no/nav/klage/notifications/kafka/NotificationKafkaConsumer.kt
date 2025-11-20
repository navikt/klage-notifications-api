package no.nav.klage.notifications.kafka

import jakarta.annotation.PostConstruct
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.getLogger
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
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
            .publishOn(Schedulers.boundedElastic())
            .doOnNext { consumerRecord ->
                try {
                    if (environment.activeProfiles.contains("dev-gcp")) {
                        logger.debug(
                            "Received Kafka-message (for persisting notification events) at offset {}: {}",
                            consumerRecord.offset(),
                            consumerRecord.value(),
                        )
                    } else {
                        logger.debug(
                            "Received Kafka-message (for persisting notification events) at offset {}",
                            consumerRecord.offset(),
                        )
                    }

                    notificationService.processNotificationMessage(
                        kafkaMessageId = UUID.fromString(consumerRecord.key()!!),
                        createNotificationEvent = consumerRecord.value(),
                    )
                    // Only acknowledge (commit offset) after successful processing
                    consumerRecord.receiverOffset().acknowledge()
                    logger.debug("Successfully processed and acknowledged message at offset {}", consumerRecord.offset())
                } catch (e: Exception) {
                    logger.error(
                        "Error processing notification message at offset {}: ${e.message}",
                        consumerRecord.offset(),
                        e
                    )
                    // Don't acknowledge - message will be reprocessed
                }
            }
            .subscribe()
    }

}