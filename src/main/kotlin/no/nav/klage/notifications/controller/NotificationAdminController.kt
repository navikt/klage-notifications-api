package no.nav.klage.notifications.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.notifications.config.SecurityConfiguration
import no.nav.klage.notifications.dto.CreateSystemNotificationRequest
import no.nav.klage.notifications.dto.TransferNotificationOwnershipRequest
import no.nav.klage.notifications.dto.view.SystemNotificationResponse
import no.nav.klage.notifications.service.NotificationService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@Tag(
    name = "admin",
    description = "API for managing notifications",
)
@ProtectedWithClaims(issuer = SecurityConfiguration.ISSUER_AAD)
@RestController
@RequestMapping("/admin/notifications")
class NotificationAdminController(
    private val notificationService: NotificationService,
) {

    /* Not in use */
//    @Operation(summary = "Delete a notification", description = "Marks a notification as deleted, by ID")
//    @ApiResponse(responseCode = "204", description = "Notification deleted successfully")
//    @ApiResponse(responseCode = "404", description = "Notification not found")
//    @DeleteMapping("/{id}")
//    fun deleteNotification(
//        @Parameter(description = "Notification ID") @PathVariable id: UUID
//    ): ResponseEntity<Void> {
//        notificationService.deleteNotification(id)
//        return ResponseEntity.noContent().build()
//    }

    @Operation(
        summary = "Delete all notifications for a behandling",
        description = "Marks all notifications related to a specific behandlingId as deleted",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Notifications deleted successfully",
    )
    @DeleteMapping("/behandling/{behandlingId}")
    fun deleteNotificationsByBehandlingId(
        @Parameter(description = "Behandling ID") @PathVariable behandlingId: UUID,
    ): ResponseEntity<Void> {
        notificationService.deleteNotificationsByBehandlingId(behandlingId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Transfer notification ownership for a behandling",
        description = "Transfers MELDING notifications to a new user (navIdent) and deletes LOST_ACCESS notifications for a specific behandlingId. " +
                "SSE clients will be notified: old owner receives DELETE events, new owner receives CREATE events.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "Notification ownership transferred successfully",
    )
    @ApiResponse(
        responseCode = "404",
        description = "No notifications found for the behandlingId",
    )
    @PostMapping("/behandling/{behandlingId}/transfer-ownership")
    fun transferNotificationOwnership(
        @Parameter(description = "Behandling ID") @PathVariable behandlingId: UUID,
        @RequestBody request: TransferNotificationOwnershipRequest,
    ): ResponseEntity<Void> {
        notificationService.transferNotificationOwnership(
            behandlingId,
            request.newNavIdent,
        )
        return ResponseEntity.ok().build()
    }

    @Operation(
        summary = "Validate no unread notifications for behandling",
        description = "Validates that there are no unread notifications for a specific behandlingId. Returns 400 if unread notifications exist.",
    )
    @ApiResponse(
        responseCode = "200",
        description = "No unread notifications found",
    )
    @ApiResponse(
        responseCode = "400",
        description = "Unread notifications exist for this behandlingId",
    )
    @GetMapping("/behandling/{behandlingId}/validate-no-unread")
    @Deprecated("Use getUnreadNotificationCount endpoint instead")
    fun validateNoUnreadNotifications(
        @Parameter(description = "Behandling ID") @PathVariable behandlingId: UUID,
    ): ResponseEntity<Void> {
        notificationService.validateNoUnreadNotificationsForBehandling(behandlingId)
        return ResponseEntity.ok().build()
    }

    @Operation(
        summary = "Create a system notification",
        description = "Creates a system-wide notification that will be sent to all users via SSE",
    )
    @ApiResponse(
        responseCode = "201",
        description = "System notification created successfully",
    )
    @PostMapping("/system")
    fun createSystemNotification(
        @RequestBody request: CreateSystemNotificationRequest,
    ): ResponseEntity<SystemNotificationResponse> {
        val notification = notificationService.createSystemNotification(request)
        val response = SystemNotificationResponse(
            id = notification.id,
            title = notification.title,
            message = notification.message,
            source = notification.source,
            createdAt = notification.createdAt,
            updatedAt = notification.updatedAt,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(
        summary = "Delete a system notification",
        description = "Marks a system notification as deleted, by ID",
    )
    @ApiResponse(
        responseCode = "204",
        description = "System notification deleted successfully",
    )
    @ApiResponse(
        responseCode = "404",
        description = "System notification not found",
    )
    @DeleteMapping("/system/{id}")
    fun deleteSystemNotification(
        @Parameter(description = "System Notification ID") @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        notificationService.deleteSystemNotification(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Delete multiple system notifications",
        description = "Marks multiple system notifications as deleted by their IDs",
    )
    @ApiResponse(
        responseCode = "204",
        description = "System notifications deleted successfully",
    )
    @ApiResponse(
        responseCode = "404",
        description = "One or more system notifications not found",
    )
    @DeleteMapping("/system")
    fun deleteMultipleSystemNotifications(
        @Parameter(description = "List of system notification IDs to delete") @RequestBody notificationIdList: List<UUID>,
    ): ResponseEntity<Void> {
        notificationService.deleteMultipleSystemNotifications(notificationIdList)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Get all system notifications",
        description = "Returns all system notifications that are not marked as deleted",
    )
    @ApiResponse(
        responseCode = "200",
        description = "System notifications retrieved successfully",
    )
    @GetMapping("/system")
    fun getAllSystemNotifications(): ResponseEntity<List<SystemNotificationResponse>> {
        val systemNotifications = notificationService.getAllSystemNotifications()
        val response = systemNotifications.map { notification ->
            SystemNotificationResponse(
                id = notification.id,
                title = notification.title,
                message = notification.message,
                source = notification.source,
                createdAt = notification.createdAt,
                updatedAt = notification.updatedAt,
            )
        }
        return ResponseEntity.ok(response)
    }
}