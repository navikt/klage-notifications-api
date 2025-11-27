package no.nav.klage.notifications.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.InetAddress

@Service
class LeaderElectionService(
    restClientBuilder: RestClient.Builder,
    @Value($$"${ELECTOR_GET_URL}")
    private val electorGetUrl: String,
) {

    private val restClient: RestClient = restClientBuilder.baseUrl(electorGetUrl).build()

    fun isLeader(): Boolean {
        val leaderElectionResponse = restClient
            .get()
            .retrieve()
            .body(LeaderElectionResponse::class.java)!!

        return leaderElectionResponse.name == InetAddress.getLocalHost().hostName
    }

}

data class LeaderElectionResponse(
    val name: String,
)