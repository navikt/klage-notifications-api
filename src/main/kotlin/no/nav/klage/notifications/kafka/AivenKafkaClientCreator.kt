package no.nav.klage.notifications.kafka

import no.nav.klage.notifications.domain.Notification
import no.nav.klage.notifications.domain.SystemNotification
import no.nav.klage.notifications.dto.CreateNotificationEvent
import no.nav.klage.notifications.dto.NotificationChangeEvent
import no.nav.klage.notifications.util.getLogger
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.internals.ConsumerFactory
import reactor.kafka.receiver.internals.DefaultKafkaReceiver
import java.util.*


@Component
class AivenKafkaClientCreator(
    @Value($$"${NOTIFICATION_EVENTS_TOPIC}")
    private val notificationEventsTopic: String,
    @Value($$"${NOTIFICATION_INTERNAL_EVENTS_TOPIC}")
    private val notificationInternalEventsTopic: String,
    @Value($$"${NOTIFICATION_INTERNAL_CHANGE_EVENTS_TOPIC}")
    private val notificationInternalChangeEventsTopic: String,
    @Value($$"${NOTIFICATION_INTERNAL_SYSTEM_EVENTS_TOPIC}")
    private val notificationInternalSystemEventsTopic: String,
    private val commonKafkaConfig: Map<String, Any>,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        val uniqueIdPerInstance = UUID.randomUUID().toString()
    }

    fun getNewKafkaNotificationEventsReceiver(): KafkaReceiver<String, CreateNotificationEvent> {
        logger.debug("Creating Kafka receiver for topic: $notificationEventsTopic")
        return defaultKafkaReceiver(
            topic = notificationEventsTopic,
            groupId = "klage-notifications-api-event-consumer",
            clientId = "klage-notifications-api-event-client",
            className = CreateNotificationEvent::class.java.name
        )
    }

    fun getNewKafkaNotificationInternalEventsReceiver(): KafkaReceiver<String, Notification> {
        logger.debug("Creating Kafka receiver for topic: $notificationInternalEventsTopic")
        return defaultKafkaReceiver(
            topic = notificationInternalEventsTopic,
            groupId = "klage-notifications-api-internal-event-consumer-$uniqueIdPerInstance",
            clientId = "klage-notifications-api-internal-event-client-$uniqueIdPerInstance",
            className = Notification::class.java.name
        )
    }

    fun getNewKafkaNotificationInternalChangeEventsReceiver(): KafkaReceiver<String, NotificationChangeEvent> {
        logger.debug("Creating Kafka receiver for topic: $notificationInternalChangeEventsTopic")
        return defaultKafkaReceiver(
            topic = notificationInternalChangeEventsTopic,
            groupId = "klage-notifications-api-internal-change-event-consumer-$uniqueIdPerInstance",
            clientId = "klage-notifications-api-internal-change-event-client-$uniqueIdPerInstance",
            className = NotificationChangeEvent::class.java.name
        )
    }

    fun getNewKafkaNotificationInternalSystemEventsReceiver(): KafkaReceiver<String, SystemNotification> {
        logger.debug("Creating Kafka receiver for topic: $notificationInternalSystemEventsTopic")
        return defaultKafkaReceiver(
            topic = notificationInternalSystemEventsTopic,
            groupId = "klage-notifications-api-internal-system-event-consumer-$uniqueIdPerInstance",
            clientId = "klage-notifications-api-internal-system-event-client-$uniqueIdPerInstance",
            className = SystemNotification::class.java.name
        )
    }

    private fun <T> defaultKafkaReceiver(
        topic: String,
        groupId: String,
        clientId: String,
        className: String,
    ): DefaultKafkaReceiver<String, T> {
        val config = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.CLIENT_ID_CONFIG to clientId,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JacksonJsonDeserializer::class.java,
            JacksonJsonDeserializer.TRUSTED_PACKAGES to "*",
            JacksonJsonDeserializer.VALUE_DEFAULT_TYPE to className
        ) + commonKafkaConfig

        return DefaultKafkaReceiver(
            ConsumerFactory.INSTANCE,
            ReceiverOptions.create<String, T>(config).subscription(listOf(topic))
        )
    }

}