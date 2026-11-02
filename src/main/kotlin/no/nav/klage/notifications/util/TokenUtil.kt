package no.nav.klage.notifications.util

import no.nav.klage.auth.currentAzureToken
import no.nav.klage.auth.navIdent
import org.springframework.stereotype.Service

@Service
class TokenUtil {
    fun getIdent(): String = currentAzureToken()?.navIdent ?: throw RuntimeException("Ident not found in token")
}
