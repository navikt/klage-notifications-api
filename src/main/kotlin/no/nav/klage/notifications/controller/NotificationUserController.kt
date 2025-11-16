package no.nav.klage.notifications.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
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

    @Operation(summary = "Mark a notification as read", description = "Marks a specific notification as read for the current user")
    @ApiResponse(responseCode = "200", description = "Notification marked as read successfully")
    @PatchMapping("/{id}/read")
    fun markAsRead(
        @Parameter(description = "Notification ID") @PathVariable id: UUID
    ) {
        val navIdent = tokenUtil.getIdent()
        notificationService.markAsRead(id = id, navIdent = navIdent)
    }

    @Operation(summary = "Mark all notifications as read", description = "Marks all notifications as read for the current user")
    @ApiResponse(responseCode = "200", description = "All notifications marked as read successfully")
    @PatchMapping("/read-all")
    fun markAllAsReadForUser() {
        val navIdent = tokenUtil.getIdent()
        notificationService.markAllAsReadForUser(navIdent)
    }

    @Operation(summary = "Mark a notification as unread", description = "Marks a specific notification as unread for the current user")
    @ApiResponse(responseCode = "200", description = "Notification marked as unread successfully")
    @PatchMapping("/{id}/unread")
    fun setUnread(
        @Parameter(description = "Notification ID") @PathVariable id: UUID
    ) {
        val navIdent = tokenUtil.getIdent()
        notificationService.setUnread(id = id, navIdent = navIdent)
    }

    @Operation(summary = "Mark multiple notifications as read", description = "Marks multiple notifications as read for the current user")
    @ApiResponse(responseCode = "200", description = "Notifications marked as read successfully")
    @PatchMapping("/read-multiple")
    fun markMultipleAsRead(
        @Parameter(description = "List of notification IDs to mark as read") @RequestBody notificationIdList: List<UUID>
    ) {
        val navIdent = tokenUtil.getIdent()
        notificationService.markMultipleAsRead(notificationIdList = notificationIdList, navIdent = navIdent)
    }
}