package no.nav.klage.notifications.config

import no.nav.klage.notifications.controller.NotificationAdminController
import no.nav.klage.notifications.controller.NotificationUserController
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun apiAdmin(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .packagesToScan(NotificationAdminController::class.java.packageName)
            .group("admin")
            .pathsToMatch("/admin/**")
            .build()
    }

    @Bean
    fun apiUser(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .packagesToScan(NotificationUserController::class.java.packageName)
            .group("user")
            .pathsToMatch("/user/**", "/notifications/**")
            .build()
    }
}
