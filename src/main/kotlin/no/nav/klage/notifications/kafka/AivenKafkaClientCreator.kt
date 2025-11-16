package no.nav.klage.notifications.kafka

import no.nav.klage.notifications.util.getLogger
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
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
    private val commonKafkaConfig: Map<String, Any>,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        val uniqueIdPerInstance = UUID.randomUUID().toString()
    }

    fun getNewKafkaNotificationEventsReceiver(): KafkaReceiver<String, String> {
        return defaultKafkaReceiver(
            topic = notificationEventsTopic,
            groupId = "klage-notifications-api-event-consumer",
            clientId = "klage-notifications-api-event-client",
        )
    }

    fun getNewKafkaNotificationInternalEventsReceiver(): KafkaReceiver<String, String> {
        return defaultKafkaReceiver(
            topic = notificationInternalEventsTopic,
            groupId = "klage-notifications-api-internal-event-consumer-$uniqueIdPerInstance",
            clientId = "klage-notifications-api-internal-event-client-$uniqueIdPerInstance",
        )
    }

    fun getNewKafkaNotificationInternalChangeEventsReceiver(): KafkaReceiver<String, String> {
        return defaultKafkaReceiver(
            topic = notificationInternalChangeEventsTopic,
            groupId = "klage-notifications-api-internal-change-event-consumer-$uniqueIdPerInstance",
            clientId = "klage-notifications-api-internal-change-event-client-$uniqueIdPerInstance",
        )
    }

    private fun defaultKafkaReceiver(
        topic: String,
        groupId: String,
        clientId: String,
    ): DefaultKafkaReceiver<String, String> {
        val config = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.CLIENT_ID_CONFIG to clientId,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to true,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ) + commonKafkaConfig

        return DefaultKafkaReceiver(
            ConsumerFactory.INSTANCE,
            ReceiverOptions.create<String, String>(config).subscription(listOf(topic))
        )
    }

}