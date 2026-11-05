package no.nav.klage.notifications.kafka

import no.nav.klage.notifications.domain.SystemNotification
import no.nav.klage.notifications.dto.InternalNotificationEvent
import no.nav.klage.notifications.dto.NotificationChangeEvent
import no.nav.klage.notifications.util.getLogger
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.UUID

/**
 * Each pod has one consumer per topic with a unique group id, so every pod receives every event.
 * Records are multicast to connected SSE clients; records arriving without clients are dropped, as
 * clients load current state from the database when they connect.
 */
@Component
class InternalKafkaEventStreams(
    @Value($$"${NOTIFICATION_INTERNAL_EVENTS_TOPIC}")
    notificationInternalEventsTopic: String,
    @Value($$"${NOTIFICATION_INTERNAL_CHANGE_EVENTS_TOPIC}")
    notificationInternalChangeEventsTopic: String,
    @Value($$"${NOTIFICATION_INTERNAL_SYSTEM_EVENTS_TOPIC}")
    notificationInternalSystemEventsTopic: String,
    private val commonKafkaConfig: Map<String, Any>,
) : SmartLifecycle {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val uniqueIdPerInstance = UUID.randomUUID().toString()
    }

    private val internalNotificationEventsSink =
        Sinks.many().multicast().directBestEffort<ConsumerRecord<String, InternalNotificationEvent?>>()
    private val internalChangeEventsSink =
        Sinks.many().multicast().directBestEffort<ConsumerRecord<String, NotificationChangeEvent?>>()
    private val internalSystemEventsSink =
        Sinks.many().multicast().directBestEffort<ConsumerRecord<String, SystemNotification?>>()

    private val internalNotificationEventsContainer =
        createContainer(
            topic = notificationInternalEventsTopic,
            groupId = "klage-notifications-api-internal-event-consumer-$uniqueIdPerInstance",
            clientId = "klage-notifications-api-internal-event-client-$uniqueIdPerInstance",
            valueType = InternalNotificationEvent::class.java,
            sink = internalNotificationEventsSink,
        )
    private val internalChangeEventsContainer =
        createContainer(
            topic = notificationInternalChangeEventsTopic,
            groupId = "klage-notifications-api-internal-change-event-consumer-$uniqueIdPerInstance",
            clientId = "klage-notifications-api-internal-change-event-client-$uniqueIdPerInstance",
            valueType = NotificationChangeEvent::class.java,
            sink = internalChangeEventsSink,
        )
    private val internalSystemEventsContainer =
        createContainer(
            topic = notificationInternalSystemEventsTopic,
            groupId = "klage-notifications-api-internal-system-event-consumer-$uniqueIdPerInstance",
            clientId = "klage-notifications-api-internal-system-event-client-$uniqueIdPerInstance",
            valueType = SystemNotification::class.java,
            sink = internalSystemEventsSink,
        )

    @Volatile
    private var running = false

    fun internalNotificationEvents(): Flux<ConsumerRecord<String, InternalNotificationEvent?>> = internalNotificationEventsSink.asFlux()

    fun internalChangeEvents(): Flux<ConsumerRecord<String, NotificationChangeEvent?>> = internalChangeEventsSink.asFlux()

    fun internalSystemEvents(): Flux<ConsumerRecord<String, SystemNotification?>> = internalSystemEventsSink.asFlux()

    override fun start() {
        internalNotificationEventsContainer.start()
        internalChangeEventsContainer.start()
        internalSystemEventsContainer.start()
        running = true
    }

    override fun stop() {
        internalNotificationEventsContainer.stop()
        internalChangeEventsContainer.stop()
        internalSystemEventsContainer.stop()
        internalNotificationEventsSink.tryEmitComplete()
        internalChangeEventsSink.tryEmitComplete()
        internalSystemEventsSink.tryEmitComplete()
        running = false
    }

    override fun isRunning(): Boolean = running

    private fun <T : Any> createContainer(
        topic: String,
        groupId: String,
        clientId: String,
        valueType: Class<T>,
        sink: Sinks.Many<ConsumerRecord<String, T?>>,
    ): KafkaMessageListenerContainer<String, T> {
        val config =
            mapOf(
                ConsumerConfig.GROUP_ID_CONFIG to groupId,
                ConsumerConfig.CLIENT_ID_CONFIG to clientId,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to JacksonJsonDeserializer::class.java.name,
                JacksonJsonDeserializer.TRUSTED_PACKAGES to "*",
                JacksonJsonDeserializer.VALUE_DEFAULT_TYPE to valueType.name,
            ) + commonKafkaConfig

        val containerProperties = ContainerProperties(topic)
        containerProperties.setMessageListener(
            MessageListener<String, T> { record ->
                @Suppress("UNCHECKED_CAST")
                val nullableRecord = record as ConsumerRecord<String, T?>
                emit(sink = sink, record = nullableRecord)
            },
        )
        containerProperties.ackMode = ContainerProperties.AckMode.BATCH
        val consumerFactory = DefaultKafkaConsumerFactory<String, T>(config)
        return KafkaMessageListenerContainer(consumerFactory, containerProperties)
    }

    private fun <T> emit(
        sink: Sinks.Many<ConsumerRecord<String, T?>>,
        record: ConsumerRecord<String, T?>,
    ) {
        val result =
            try {
                sink.tryEmitNext(record)
            } catch (e: Exception) {
                logger.warn(
                    "Could not emit Kafka record from topic {} partition {} offset {}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    e,
                )
                return
            }

        if (result != Sinks.EmitResult.OK && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            logger.warn(
                "Could not emit Kafka record from topic {} partition {} offset {}: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                result,
            )
        }
    }
}
