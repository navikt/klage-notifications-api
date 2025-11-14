package no.nav.klage.notifications.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.notifications.config.SecurityConfiguration
import no.nav.klage.notifications.dto.NotificationResponse
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.TokenUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "user", description = "API for user notifications")
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
@RestController
@RequestMapping("/user/notifications")
class NotificationUserController(
    private val notificationService: NotificationService,
    private val tokenUtil: TokenUtil,
) {

    //add events endpoint for SSE clients to listen to user notifications in real-time

    @GetMapping
    fun getNotificationsByUser(
        @RequestParam(required = false) read: Boolean?
    ): ResponseEntity<List<NotificationResponse>> {
        val navIdent = tokenUtil.getIdent()
        return ResponseEntity.ok(notificationService.getNotificationsByNavIdentAndRead(navIdent, read ?: false))
    }

    //add endpoint to set multiple notifications as read, by IDs
//    @PatchMapping("/read")
//    fun markMultipleAsRead(@RequestBody ids: List<UUID>): ResponseEntity<List<NotificationResponse>> {
//        return ResponseEntity.ok(notificationService.markMultipleAsRead(ids))
//    }

    @PatchMapping("/{id}/read")
    fun markAsRead(@PathVariable id: UUID): ResponseEntity<NotificationResponse> {
        return ResponseEntity.ok(notificationService.markAsRead(id))
    }

    @PatchMapping("/read-all")
    fun markAllAsReadForUser(): ResponseEntity<List<NotificationResponse>> {
        val navIdent = tokenUtil.getIdent()
        return ResponseEntity.ok(notificationService.markAllAsReadForUser(navIdent))
    }

    @PatchMapping("/{id}/unread")
    fun setUnread(@PathVariable id: UUID): ResponseEntity<NotificationResponse> {
        return ResponseEntity.ok(notificationService.setUnread(id))
    }
}