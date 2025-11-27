package no.nav.klage.notifications.client

import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange("leader-election")
interface LeaderElectionClient {

    @GetExchange
    fun getLeaderHostname(): LeaderElectionResponse
}

data class LeaderElectionResponse(
    val name: String,
)