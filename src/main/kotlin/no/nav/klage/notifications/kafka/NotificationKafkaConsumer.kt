package no.nav.klage.notifications.kafka

import no.nav.klage.notifications.dto.CreateNotificationRequest
import no.nav.klage.notifications.dto.KafkaNotificationMessage
import no.nav.klage.notifications.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class NotificationKafkaConsumer(
    private val notificationService: NotificationService
) {

    private val logger = LoggerFactory.getLogger(NotificationKafkaConsumer::class.java)

//    @KafkaListener(topics = ["notifications"], groupId = "klage-notification-api")
//    fun consume(message: KafkaNotificationMessage) {
//        logger.debug("Received notification from Kafka: {}", message)
//
//        try {
//            val request = CreateNotificationRequest(
//                title = message.title,
//                message = message.message,
//                navIdent = message.navIdent,
//                severity = message.severity,
//                source = message.source,
//            )
//
//            notificationService.createNotification(request)
//            logger.debug("Successfully processed Kafka notification")
//        } catch (e: Exception) {
//            logger.error("Error processing Kafka notification", e)
//        }
//    }
}