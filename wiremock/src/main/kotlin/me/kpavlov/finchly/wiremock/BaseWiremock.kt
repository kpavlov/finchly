package me.kpavlov.finchly.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.VerificationException

public abstract class BaseWiremock(
    protected val mock: WireMockServer,
) {
    init {
        mock.start()
    }

    public fun port(): Int = mock.port()

    public fun resetStub(mapping: com.github.tomakehurst.wiremock.stubbing.StubMapping) {
        mock.removeStub(mapping)
    }

    public fun reset() {
        mock.resetAll()
    }

    public fun verifyNoUnmatchedRequests() {
        val unmatchedRequests = mock.findAllUnmatchedRequests()
        if (unmatchedRequests.isEmpty()) {
            return
        }
        println("Unmatched requests: $unmatchedRequests")
        val nearMisses = mock.findNearMissesForAllUnmatchedRequests()
        if (nearMisses.isEmpty()) {
            throw VerificationException.forUnmatchedRequests(unmatchedRequests)
        } else {
            throw VerificationException.forUnmatchedNearMisses(nearMisses)
        }
    }
}
