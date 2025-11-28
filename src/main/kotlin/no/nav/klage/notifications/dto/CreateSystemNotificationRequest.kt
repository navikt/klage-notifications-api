package no.nav.klage.notifications.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateSystemNotificationRequest(
    val title: String,
    val message: String,
)