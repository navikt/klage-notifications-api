package no.nav.klage.notifications.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.notifications.config.SecurityConfiguration
import no.nav.klage.notifications.service.NotificationService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(name = "admin", description = "API for managing notifications")
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
@RestController
@RequestMapping("/admin/notifications")
class NotificationAdminController(
    private val notificationService: NotificationService
) {

    @Operation(summary = "Delete a notification", description = "Marks a notification as deleted, by ID")
    @ApiResponse(responseCode = "204", description = "Notification deleted successfully")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    @DeleteMapping("/{id}")
    fun deleteNotification(
        @Parameter(description = "Notification ID") @PathVariable id: UUID
    ): ResponseEntity<Void> {
        notificationService.deleteNotification(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Delete all notifications for a behandling",
        description = "Marks all notifications related to a specific behandlingId as deleted"
    )
    @ApiResponse(responseCode = "200", description = "Notifications deleted successfully")
    @DeleteMapping("/behandling/{behandlingId}")
    fun deleteNotificationsByBehandlingId(
        @Parameter(description = "Behandling ID") @PathVariable behandlingId: UUID
    ): ResponseEntity<Void> {
        notificationService.deleteNotificationsByBehandlingId(behandlingId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Validate no unread notifications for behandling",
        description = "Validates that there are no unread notifications for a specific behandlingId. Returns 400 if unread notifications exist."
    )
    @ApiResponse(responseCode = "200", description = "No unread notifications found")
    @ApiResponse(responseCode = "400", description = "Unread notifications exist for this behandlingId")
    @GetMapping("/behandling/{behandlingId}/validate-no-unread")
    fun validateNoUnreadNotifications(
        @Parameter(description = "Behandling ID") @PathVariable behandlingId: UUID
    ): ResponseEntity<Void> {
        notificationService.validateNoUnreadNotificationsForBehandling(behandlingId)
        return ResponseEntity.ok().build()
    }
}