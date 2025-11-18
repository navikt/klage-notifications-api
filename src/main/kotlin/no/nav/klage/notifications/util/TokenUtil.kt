package no.nav.klage.notifications.util

import no.nav.klage.notifications.config.SecurityConfiguration
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Service

@Service
class TokenUtil(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getIdent(): String =
        tokenValidationContextHolder.getTokenValidationContext().getJwtToken(SecurityConfiguration.ISSUER_AAD)
            ?.jwtTokenClaims?.get("NAVident")?.toString()
            ?: throw RuntimeException("Ident not found in token")
}