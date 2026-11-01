package no.nav.klage.notifications.config

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import no.nav.klage.auth.test.MockAzure
import no.nav.klage.notifications.controller.SSEEventController
import no.nav.klage.notifications.kafka.AivenKafkaClientCreator
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
import reactor.kafka.receiver.KafkaReceiver

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

    @MockkBean
    lateinit var aivenKafkaClientCreator: AivenKafkaClientCreator

    @MockkBean
    lateinit var notificationService: NotificationService

    @BeforeEach
    fun setup() {
        every { aivenKafkaClientCreator.getNewKafkaNotificationInternalEventsReceiver() } returns neverEmittingReceiver()
        every { aivenKafkaClientCreator.getNewKafkaNotificationInternalChangeEventsReceiver() } returns neverEmittingReceiver()
        every { aivenKafkaClientCreator.getNewKafkaNotificationInternalSystemEventsReceiver() } returns neverEmittingReceiver()
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

    private fun <V : Any> neverEmittingReceiver(): KafkaReceiver<String, V> = mockk { every { receive() } returns Flux.never() }
}
