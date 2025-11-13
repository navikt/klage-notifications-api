package no.nav.klage.notifications.util

import no.nav.klage.notifications.config.SecurityConfiguration
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Service

@Service
class TokenUtil(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val clientConfigurationProperties: ClientConfigurationProperties,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getAccessTokenFrontendSent(): String =
        tokenValidationContextHolder.getTokenValidationContext().getJwtToken(SecurityConfiguration.ISSUER_AAD)!!.encodedToken

    fun getIdent(): String =
        tokenValidationContextHolder.getTokenValidationContext().getJwtToken(SecurityConfiguration.ISSUER_AAD)
            ?.jwtTokenClaims?.get("NAVident")?.toString()
            ?: throw RuntimeException("Ident not found in token")

    fun getCallingApplication(): String =
        tokenValidationContextHolder.getTokenValidationContext().getJwtToken(SecurityConfiguration.ISSUER_AAD)
            ?.jwtTokenClaims?.get("azp_name")?.toString()
            ?: throw RuntimeException("Application not found in token")
}