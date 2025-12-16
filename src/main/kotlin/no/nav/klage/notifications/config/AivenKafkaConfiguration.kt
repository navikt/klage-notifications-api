package no.nav.klage.notifications.config

import no.nav.klage.notifications.dto.CreateNotificationEvent
import no.nav.klage.notifications.util.getLogger
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonSerializer

@EnableKafka
@Configuration
class AivenKafkaConfiguration(
    @Value($$"${KAFKA_BROKERS}")
    private val kafkaBrokers: String,
    @Value($$"${KAFKA_TRUSTSTORE_PATH}")
    private val kafkaTruststorePath: String,
    @Value($$"${KAFKA_CREDSTORE_PASSWORD}")
    private val kafkaCredstorePassword: String,
    @Value($$"${KAFKA_KEYSTORE_PATH}")
    private val kafkaKeystorePath: String,
    @Value($$"${spring.profiles.active:local}")
    private val springProfile: String,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    //Common config bean
    @Bean
    fun commonKafkaConfig(): Map<String, Any> = mapOf(
        BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers
    ) + securityConfig()

    //Producer bean
    @Bean
    fun aivenKafkaTemplate(commonKafkaConfig: Map<String, Any>): KafkaTemplate<String, Any> {
        val config = mapOf(
            ProducerConfig.CLIENT_ID_CONFIG to "klage-notifications-api-$springProfile-producer",
            ProducerConfig.ACKS_CONFIG to "1",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonJsonSerializer::class.java
        ) + commonKafkaConfig

        return KafkaTemplate(DefaultKafkaProducerFactory(config))
    }

    //Consumer factory bean
    @Bean
    fun consumerFactory(commonKafkaConfig: Map<String, Any>): ConsumerFactory<String, CreateNotificationEvent> {
        val config = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to "klage-notifications-api-$springProfile-consumer",
            ConsumerConfig.CLIENT_ID_CONFIG to "klage-notifications-api-$springProfile-client",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to JacksonJsonDeserializer::class.java.name,
            JacksonJsonDeserializer.TRUSTED_PACKAGES to "*",
            JacksonJsonDeserializer.VALUE_DEFAULT_TYPE to CreateNotificationEvent::class.java.name
        ) + commonKafkaConfig

        return DefaultKafkaConsumerFactory(config)
    }

    //Kafka listener container factory bean
    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, CreateNotificationEvent>
    ): ConcurrentKafkaListenerContainerFactory<String, CreateNotificationEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, CreateNotificationEvent>()
        factory.setConsumerFactory(consumerFactory)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.setConcurrency(1) // Single consumer thread to maintain ordering
        return factory
    }

    private fun securityConfig() = mapOf(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
        SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "", // Disable server host name verification
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
        SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword,
    )

}