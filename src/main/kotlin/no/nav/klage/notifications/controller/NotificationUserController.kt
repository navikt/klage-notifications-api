package no.nav.klage.notifications.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.notifications.config.SecurityConfiguration
import no.nav.klage.notifications.domain.NotificationStatus
import no.nav.klage.notifications.dto.NotificationResponse
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.TokenUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "user", description = "API for user notifications")
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
@RestController
@RequestMapping("/api/user/notifications")
class NotificationUserController(
    private val notificationService: NotificationService,
    private val tokenUtil: TokenUtil,
) {

    @GetMapping
    fun getNotificationsByUser(
        @RequestParam(required = false) status: NotificationStatus?
    ): ResponseEntity<List<NotificationResponse>> {
        val navIdent = tokenUtil.getIdent()
        return ResponseEntity.ok(notificationService.getNotificationsByNavIdentAndStatus(navIdent, status ?: NotificationStatus.UNREAD))
    }

    @GetMapping("/{id}")
    fun getNotificationById(@PathVariable id: UUID): ResponseEntity<NotificationResponse> {
        return ResponseEntity.ok(notificationService.getNotificationById(id))
    }

    @PatchMapping("/{id}/read")
    fun markAsRead(@PathVariable id: UUID): ResponseEntity<NotificationResponse> {
        return ResponseEntity.ok(notificationService.markAsRead(id))
    }

    @PatchMapping("/read-all")
    fun markAllAsReadForUser(): ResponseEntity<List<NotificationResponse>> {
        val navIdent = "SOME_NAV_IDENT"
        return ResponseEntity.ok(notificationService.markAllAsReadForUser(navIdent))
    }

    @PatchMapping("/{id}/unread")
    fun setUnread(@PathVariable id: UUID): ResponseEntity<NotificationResponse> {
        return ResponseEntity.ok(notificationService.setUnread(id))
    }

    @PatchMapping("/{id}/archived")
    fun setArchived(@PathVariable id: UUID): ResponseEntity<NotificationResponse> {
        return ResponseEntity.ok(notificationService.setArchived(id))
    }

    @DeleteMapping("/{id}")
    fun markNotificationAsDeleted(@PathVariable id: UUID): ResponseEntity<Void> {
        notificationService.deleteNotification(id)
        return ResponseEntity.noContent().build()
    }
}