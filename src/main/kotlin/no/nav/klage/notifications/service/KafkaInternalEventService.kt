package no.nav.klage.notifications.service

import no.nav.klage.notifications.domain.Notification
import no.nav.klage.notifications.dto.NotificationChangeEvent
import no.nav.klage.notifications.util.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaInternalEventService(
    private val aivenKafkaTemplate: KafkaTemplate<String, Any>,
    @Value($$"${NOTIFICATION_INTERNAL_EVENTS_TOPIC}")
    private val notificationInternalEventsTopic: String,
    @Value($$"${NOTIFICATION_INTERNAL_CHANGE_EVENTS_TOPIC}")
    private val notificationInternalChangeEventsTopic: String,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun publishInternalNotificationEvent(notification: Notification) {
        runCatching {
            logger.debug("Publishing internalNotificationEvent to Kafka for subscribers")

            aivenKafkaTemplate.send(
                notificationInternalEventsTopic,
                notification.id.toString(),
                notification,
            ).get()
            logger.debug("Published internalNotificationEvent to Kafka for subscribers")
        }.onFailure {
            logger.error("Could not publish internalNotificationEvent to subscribers", it)
        }
    }

    fun publishInternalNotificationChangeEvent(notificationChangeEvent: NotificationChangeEvent) {
        runCatching {
            logger.debug("Publishing internalNotificationChangeEvent to Kafka for subscribers")

            aivenKafkaTemplate.send(
                notificationInternalChangeEventsTopic,
                notificationChangeEvent.id.toString(),
                notificationChangeEvent,
            ).get()
            logger.debug("Published internalNotificationChangeEvent to Kafka for subscribers")
        }.onFailure {
            logger.error("Could not publish internalNotificationChangeEvent to subscribers", it)
        }
    }
}