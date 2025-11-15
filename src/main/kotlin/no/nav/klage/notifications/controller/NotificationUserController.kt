package no.nav.klage.notifications.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.notifications.config.SecurityConfiguration
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.TokenUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@Tag(name = "user", description = "API for user notifications")
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
@RestController
@RequestMapping("/user/notifications")
class NotificationUserController(
    private val notificationService: NotificationService,
    private val tokenUtil: TokenUtil,
) {

    //add endpoint to set multiple notifications as read, by IDs
//    @PatchMapping("/read")
//    fun markMultipleAsRead(@RequestBody ids: List<UUID>): ResponseEntity<List<NotificationResponse>> {
//        return ResponseEntity.ok(notificationService.markMultipleAsRead(ids))
//    }

    @PatchMapping("/{id}/read")
    fun markAsRead(@PathVariable id: UUID) {
        notificationService.markAsRead(id)
    }

    @PatchMapping("/read-all")
    fun markAllAsReadForUser() {
        val navIdent = tokenUtil.getIdent()
        notificationService.markAllAsReadForUser(navIdent)
    }

    @PatchMapping("/{id}/unread")
    fun setUnread(@PathVariable id: UUID) {
        notificationService.setUnread(id)
    }
}