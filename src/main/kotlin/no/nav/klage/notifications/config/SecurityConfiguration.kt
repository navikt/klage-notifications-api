package no.nav.klage.notifications.config

import no.nav.klage.auth.AzureAuthProperties
import no.nav.klage.auth.azureJwtDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    companion object {
        // Swagger is only enabled in dev (application-dev.yaml). In prod these paths give 404.
        val UNAUTHENTICATED_PATHS =
            listOf(
                "/internal/health",
                "/internal/health/**",
                "/internal/info",
                "/internal/prometheus",
                "/error",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
            )
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it
                    .requestMatchers(*UNAUTHENTICATED_PATHS.toTypedArray())
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.oauth2ResourceServer { it.jwt { } }
            .build()

    @Bean
    fun jwtDecoder(environment: Environment): JwtDecoder = azureJwtDecoder(AzureAuthProperties.from(environment))
}
