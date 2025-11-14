package no.nav.klage.notifications.service

import com.fasterxml.jackson.databind.JsonNode
import no.nav.klage.notifications.domain.LostAccessNotification
import no.nav.klage.notifications.domain.MeldingNotification
import no.nav.klage.notifications.domain.Notification
import no.nav.klage.notifications.domain.NotificationType
import no.nav.klage.notifications.dto.CreateLostAccessNotificationRequest
import no.nav.klage.notifications.dto.CreateMeldingNotificationRequest
import no.nav.klage.notifications.dto.NotificationResponse
import no.nav.klage.notifications.exceptions.NotificationNotFoundException
import no.nav.klage.notifications.repository.LostAccessNotificationRepository
import no.nav.klage.notifications.repository.MeldingNotificationRepository
import no.nav.klage.notifications.repository.NotificationRepository
import no.nav.klage.notifications.util.getLogger
import no.nav.klage.notifications.util.ourJacksonObjectMapper
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val meldingNotificationRepository: MeldingNotificationRepository,
    private val lostAccessNotificationRepository: LostAccessNotificationRepository,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)

        private val objectMapper = ourJacksonObjectMapper()
    }

//    @Transactional(readOnly = true)
//    fun getAllNotifications(): List<NotificationResponse> {
//        return notificationRepository.findAll().map { it.toResponse() }
//    }
//
//    @Transactional(readOnly = true)
//    fun getNotificationsByNavIdent(navIdent: String): List<NotificationResponse> {
//        return notificationRepository.findByNavIdentOrderByCreatedAtAsc(navIdent)
//            .map { it.toResponse() }
//    }

    @Transactional(readOnly = true)
    fun getNotificationsByNavIdentAndRead(navIdent: String, read: Boolean): List<NotificationResponse> {
        return notificationRepository.findByNavIdentAndReadOrderByCreatedAtAsc(navIdent, read)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getNotificationById(id: UUID): NotificationResponse {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }
        return notification.toResponse()
    }

    fun markAsRead(id: UUID): NotificationResponse {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }

        notification.read = true
        notification.readAt = LocalDateTime.now()
        notification.updatedAt = LocalDateTime.now()

        val saved = notificationRepository.save(notification)
        val response = saved.toResponse()

        return response
    }

    fun setUnread(id: UUID): NotificationResponse {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }

        notification.read = false
        notification.updatedAt = LocalDateTime.now()

        val saved = notificationRepository.save(notification)
        val response = saved.toResponse()

        return response
    }

    fun markAllAsReadForUser(navIdent: String): List<NotificationResponse> {
        val notifications =
            notificationRepository.findByNavIdentAndReadOrderByCreatedAtAsc(navIdent, false)

        notifications.forEach { notification ->
            notification.read = true
            notification.readAt = LocalDateTime.now()
            notification.updatedAt = LocalDateTime.now()
        }

        val saved = notificationRepository.saveAll(notifications)
        val responses = saved.map { it.toResponse() }

        return responses
    }

    fun deleteNotification(id: UUID) {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }

        notification.markedAsDeleted = true
        notification.updatedAt = LocalDateTime.now()
    }

    private fun Notification.toResponse() = NotificationResponse(
        id = this.id,
        message = this.message,
        navIdent = this.navIdent,
        read = this.read,
        source = this.source,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        readAt = this.readAt,
        markedAsDeleted = this.markedAsDeleted,
    )

    fun processNotificationMessage(messageId: UUID, jsonNode: JsonNode) {
        try {
            val type = NotificationType.valueOf(jsonNode.get("type").asText())
            logger.debug("Processing notification message with id {} of type {}", messageId, type)

            when (type) {
                NotificationType.MELDING -> {
                    val request = objectMapper.treeToValue(jsonNode, CreateMeldingNotificationRequest::class.java)
                    createMeldingNotification(request, messageId)
                }

                NotificationType.LOST_ACCESS -> {
                    val request = objectMapper.treeToValue(jsonNode, CreateLostAccessNotificationRequest::class.java)
                    createLostAccessNotification(request, messageId)
                }
            }
            logger.debug("Successfully processed notification message with id {}", messageId)
        } catch (e: DataIntegrityViolationException) {
            // Message already processed by another instance
            logger.debug("Message {} already processed", messageId)
        } catch (e: Exception) {
            logger.error("Error processing notification message with id $messageId: ${e.message}", e)
            throw e
        }
    }

    fun createMeldingNotification(request: CreateMeldingNotificationRequest, kafkaMessageId: UUID) {
        val now = LocalDateTime.now()
        val notification = MeldingNotification(
            message = request.message,
            navIdent = request.recipientNavIdent,
            read = false,
            source = request.source,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = kafkaMessageId,
            behandlingId = request.behandlingId,
            meldingId = request.meldingId,
            actorNavIdent = request.actorNavIdent,
            actorNavn = request.actorNavn,
            saksnummer = request.saksnummer,
            ytelse = request.ytelse,
            meldingCreated = request.meldingCreated,
            behandlingType = request.behandlingType,
        )

        meldingNotificationRepository.save(notification)
    }

    fun createLostAccessNotification(request: CreateLostAccessNotificationRequest, kafkaMessageId: UUID) {
        val now = LocalDateTime.now()
        val notification = LostAccessNotification(
            message = request.message,
            navIdent = request.navIdent,
            read = false,
            source = request.source,
            createdAt = now,
            updatedAt = now,
            readAt = null,
            markedAsDeleted = false,
            kafkaMessageId = kafkaMessageId,
            behandlingId = request.behandlingId,
            saksnummer = request.saksnummer,
            ytelse = request.ytelse,
            behandlingType = request.behandlingType,
        )

        lostAccessNotificationRepository.save(notification)
    }
}