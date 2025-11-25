package no.nav.klage.notifications.service

import no.nav.klage.notifications.domain.Notification
import no.nav.klage.notifications.domain.SystemNotification
import no.nav.klage.notifications.dto.InternalNotificationEvent
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
    @Value($$"${NOTIFICATION_INTERNAL_SYSTEM_EVENTS_TOPIC}")
    private val notificationInternalSystemEventsTopic: String,
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

    fun publishInternalNotificationEvents(notifications: List<Notification>) {
        runCatching {
            logger.debug("Publishing {} internalNotificationEvents to Kafka for subscribers", notifications.size)

            val event = InternalNotificationEvent(notifications)
            val key = if (notifications.isNotEmpty()) {
                "notification-bulk-${notifications.first().navIdent}"
            } else {
                notifications.first().id.toString()
            }
            aivenKafkaTemplate.send(
                notificationInternalEventsTopic,
                key,
                event,
            ).get()

            logger.debug("Published {} internalNotificationEvents to Kafka for subscribers", notifications.size)
        }.onFailure {
            logger.error("Could not publish internalNotificationEvents to subscribers", it)
        }
    }

    fun publishInternalNotificationChangeEvent(notificationChangeEvent: NotificationChangeEvent) {
        runCatching {
            logger.debug("Publishing internalNotificationChangeEvent to Kafka for subscribers")

            val key = if (!notificationChangeEvent.ids.isNullOrEmpty()) {
                "notification-bulk-change-${notificationChangeEvent.navIdent}"
            } else {
                notificationChangeEvent.id!!.toString()
            }

            aivenKafkaTemplate.send(
                notificationInternalChangeEventsTopic,
                key,
                notificationChangeEvent,
            ).get()
            logger.debug("Published internalNotificationChangeEvent to Kafka for subscribers")
        }.onFailure {
            logger.error("Could not publish internalNotificationChangeEvent to subscribers", it)
        }
    }

    fun publishSystemNotificationEvent(systemNotification: SystemNotification) {
        runCatching {
            logger.debug("Publishing system notification event to Kafka for SSE subscribers")

            aivenKafkaTemplate.send(
                notificationInternalSystemEventsTopic,
                systemNotification.id.toString(),
                systemNotification,
            ).get()
            logger.debug("Published system notification event to Kafka for SSE subscribers")
        }.onFailure {
            logger.error("Could not publish system notification event to subscribers", it)
        }
    }
}