package no.nav.klage.notifications.service

import com.fasterxml.jackson.databind.JsonNode
import no.nav.klage.notifications.util.getLogger
import no.nav.klage.notifications.util.ourJacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class KafkaInternalEventService(
    private val aivenKafkaTemplate: KafkaTemplate<String, String>,
    @Value($$"${NOTIFICATION_INTERNAL_EVENTS_TOPIC}")
    private val notificationInternalEventsTopic: String,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val objectMapper = ourJacksonObjectMapper()
    }

    fun publishInternalNotificationEvent(jsonNode: JsonNode) {
        runCatching {
            logger.debug("Publishing internalNotificationEvent to Kafka for subscribers")

            aivenKafkaTemplate.send(
                notificationInternalEventsTopic,
                jsonNode.get("id").asText(),
                objectMapper.writeValueAsString(jsonNode)
            ).get()
            logger.debug("Published internalNotificationEvent to Kafka for subscribers")
        }.onFailure {
            logger.error("Could not publish internalNotificationEvent to subscribers", it)
        }
    }
}