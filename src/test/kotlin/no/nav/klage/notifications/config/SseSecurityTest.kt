package no.nav.klage.notifications.config

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.klage.auth.test.MockAzure
import no.nav.klage.notifications.controller.SSEEventController
import no.nav.klage.notifications.kafka.InternalKafkaEventStreams
import no.nav.klage.notifications.service.NotificationService
import no.nav.klage.notifications.util.TokenUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import reactor.core.publisher.Flux

/**
 * The SSE endpoint returns a Flux, which Spring MVC handles as an async request. Verifies that a
 * valid token opens the stream and that NAVident is read on the request thread.
 */
@WebMvcTest(SSEEventController::class)
@Import(SecurityConfiguration::class, TokenUtil::class, SseShutdownSignal::class)
class SseSecurityTest {
    companion object {
        private val mockAzure = MockAzure()

        @JvmStatic
        @DynamicPropertySource
        fun azureProperties(registry: DynamicPropertyRegistry) {
            mockAzure.registerProperties(registry)
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var sseShutdownSignal: SseShutdownSignal

    @MockkBean(relaxed = true)
    lateinit var internalKafkaEventStreams: InternalKafkaEventStreams

    @MockkBean
    lateinit var notificationService: NotificationService

    @BeforeEach
    fun setup() {
        every { internalKafkaEventStreams.internalNotificationEvents() } returns Flux.never()
        every { internalKafkaEventStreams.internalChangeEvents() } returns Flux.never()
        every { internalKafkaEventStreams.internalSystemEvents() } returns Flux.never()
        every { notificationService.getNotificationsByNavIdent(any()) } returns emptyList()
        every { notificationService.getAllSystemNotifications() } returns emptyList()
    }

    @Test
    fun `stream requires token`() {
        mockMvc.perform(get("/user/notifications/events")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `valid token opens the stream`() {
        val result =
            mockMvc
                .perform(
                    get("/user/notifications/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${mockAzure.token(claims = mapOf("NAVident" to "Z123456"))}"),
                ).andExpect(request().asyncStarted())
                .andReturn()

        sseShutdownSignal.onShutdown()

        mockMvc
            .perform(asyncDispatch(result))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("event:HEARTBEAT")))
    }
}
