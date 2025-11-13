package no.nav.klage.notifications.service

import no.nav.klage.notifications.domain.Notification
import no.nav.klage.notifications.domain.NotificationStatus
import no.nav.klage.notifications.dto.CreateNotificationRequest
import no.nav.klage.notifications.dto.NotificationResponse
import no.nav.klage.notifications.dto.UpdateNotificationRequest
import no.nav.klage.notifications.exceptions.NotificationNotFoundException
import no.nav.klage.notifications.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {

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
    fun getNotificationsByNavIdentAndStatus(navIdent: String, status: NotificationStatus): List<NotificationResponse> {
        return notificationRepository.findByNavIdentAndStatusOrderByCreatedAtAsc(navIdent, status)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getNotificationById(id: UUID): NotificationResponse {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }
        return notification.toResponse()
    }

//    @Transactional
//    fun createNotification(
//        request: CreateNotificationRequest,
//    ): NotificationResponse {
//        val notification = Notification(
//            title = request.title,
//            message = request.message,
//            navIdent = request.navIdent,
//            severity = request.severity,
//            source = request.source,
//            status = NotificationStatus.UNREAD,
//            createdAt = LocalDateTime.now(),
//            updatedAt = LocalDateTime.now(),
//            readAt = null,
//            markedAsDeleted = false,
//        )
//
//        val saved = notificationRepository.save(notification)
//        val response = saved.toResponse()
//
//        return response
//    }

//    @Transactional
//    fun updateNotification(id: UUID, request: UpdateNotificationRequest): NotificationResponse {
//        val notification = notificationRepository.findById(id)
//            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }
//
//        request.title?.let { notification.title = it }
//        request.message?.let { notification.message = it }
//        request.severity?.let { notification.severity = it }
//        request.status?.let {
//            notification.status = it
//            if (it == NotificationStatus.READ && notification.readAt == null) {
//                notification.readAt = LocalDateTime.now()
//            }
//        }
//        notification.updatedAt = LocalDateTime.now()
//
//        val saved = notificationRepository.save(notification)
//        val response = saved.toResponse()
//
//        return response
//    }

    @Transactional
    fun markAsRead(id: UUID): NotificationResponse {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }

        notification.status = NotificationStatus.READ
        notification.readAt = LocalDateTime.now()
        notification.updatedAt = LocalDateTime.now()

        val saved = notificationRepository.save(notification)
        val response = saved.toResponse()

        return response
    }

    @Transactional
    fun setUnread(id: UUID): NotificationResponse {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }

        notification.status = NotificationStatus.UNREAD
        notification.updatedAt = LocalDateTime.now()

        val saved = notificationRepository.save(notification)
        val response = saved.toResponse()

        return response
    }

    @Transactional
    fun setArchived(id: UUID): NotificationResponse {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }

        notification.status = NotificationStatus.ARCHIVED
        notification.updatedAt = LocalDateTime.now()

        val saved = notificationRepository.save(notification)
        val response = saved.toResponse()

        return response
    }

    @Transactional
    fun markAllAsReadForUser(navIdent: String): List<NotificationResponse> {
        val notifications = notificationRepository.findByNavIdentAndStatusOrderByCreatedAtAsc(navIdent, NotificationStatus.UNREAD)

        notifications.forEach { notification ->
            notification.status = NotificationStatus.READ
            notification.readAt = LocalDateTime.now()
            notification.updatedAt = LocalDateTime.now()
        }

        val saved = notificationRepository.saveAll(notifications)
        val responses = saved.map { it.toResponse() }

        return responses
    }

    @Transactional
    fun deleteNotification(id: UUID) {
        val notification = notificationRepository.findById(id)
            .orElseThrow { NotificationNotFoundException("Notification with id $id not found") }

        notification.markedAsDeleted = true
        notification.updatedAt = LocalDateTime.now()
    }

    private fun Notification.toResponse() = NotificationResponse(
        id = this.id,
        title = this.title,
        message = this.message,
        navIdent = this.navIdent,
        severity = this.severity,
        status = this.status,
        source = this.source,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        readAt = this.readAt,
        markedAsDeleted = this.markedAsDeleted,
    )
}