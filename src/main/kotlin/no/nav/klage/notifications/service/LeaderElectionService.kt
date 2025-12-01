package no.nav.klage.notifications.service

import no.nav.klage.notifications.util.getLogger
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

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun isLeader(): Boolean {
        val leaderElectionResponse = restClient
            .get()
            .retrieve()
            .body(LeaderElectionResponse::class.java)!!

        val isLeader = leaderElectionResponse.name == InetAddress.getLocalHost().hostName
        logger.debug("Leader election check: host = ${InetAddress.getLocalHost().hostName}, leader = ${leaderElectionResponse.name}, isLeader = $isLeader")

        return isLeader
    }

}

data class LeaderElectionResponse(
    val name: String,
)