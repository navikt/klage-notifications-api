package no.nav.klage.notifications.dto

data class UpdateNotificationRequest(
    val message: String? = null,
    val read: Boolean? = null,
)