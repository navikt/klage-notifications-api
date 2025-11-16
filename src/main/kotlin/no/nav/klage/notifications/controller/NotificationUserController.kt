package no.nav.klage.notifications.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.notifications.config.SecurityConfiguration
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.TokenUtil
import no.nav.security.token.support.core.api.ProtectedWithClaims
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

    @PatchMapping("/{id}/read")
    fun markAsRead(@PathVariable id: UUID) {
        val navIdent = tokenUtil.getIdent()
        notificationService.markAsRead(id = id, navIdent = navIdent)
    }

    @PatchMapping("/read-all")
    fun markAllAsReadForUser() {
        val navIdent = tokenUtil.getIdent()
        notificationService.markAllAsReadForUser(navIdent)
    }

    @PatchMapping("/{id}/unread")
    fun setUnread(@PathVariable id: UUID) {
        val navIdent = tokenUtil.getIdent()
        notificationService.setUnread(id = id, navIdent = navIdent)
    }

    @PatchMapping("/read-multiple")
    fun markMultipleAsRead(@RequestBody notificationIdList: List<UUID>) {
        val navIdent = tokenUtil.getIdent()
        notificationService.markMultipleAsRead(notificationIdList = notificationIdList, navIdent = navIdent)
    }
}