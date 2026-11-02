package no.nav.klage.notifications.config

import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import no.nav.klage.auth.test.AzureSecurityContractTest
import no.nav.klage.notifications.controller.NotificationUserController
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.TokenUtil
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(NotificationUserController::class)
@Import(SecurityConfiguration::class, TokenUtil::class)
class SecurityConfigurationTest : AzureSecurityContractTest() {
    @MockkBean(relaxed = true)
    lateinit var notificationService: NotificationService

    override fun protectedRequest() = get("/notifications/behandling/${UUID.randomUUID()}/unread-count")

    @Test
    fun `marks notification as read for NAVident in token`() {
        val id = UUID.randomUUID()

        mockMvc
            .perform(
                patch("/notifications/$id/read")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${mockAzure.token(claims = mapOf("NAVident" to "Z123456"))}"),
            ).andExpect(status().isOk)

        verify { notificationService.markAsRead(id = id, navIdent = "Z123456") }
    }
}
