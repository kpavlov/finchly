package me.kpavlov.finchly.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.VerificationException

/**
 * Base class for managing a WireMock server instance.
 *
 * The server is started upon initialization.
 * Provides utility methods for managing stub mappings and verifying unmatched requests.
 *
 * @param mock The WireMock server instance to manage.
 */
public abstract class BaseWiremock(
    protected val mock: WireMockServer,
) {
    init {
        mock.start()
    }

    /**
     * Retrieves the port number on which the WireMock server instance is running.
     *
     * @return the port number on which the WireMock server instance is listening.
     */
    public fun port(): Int = mock.port()

    /**
     * Removes a specific stub mapping from the WireMock server.
     *
     * @param mapping The stub mapping to remove.
     */
    public fun resetStub(mapping: com.github.tomakehurst.wiremock.stubbing.StubMapping) {
        mock.removeStub(mapping)
    }

    /**
     * Resets all the stub mappings in the WireMock server instance.
     *
     * This function clears all the stub mappings that have been set up in the WireMock server.
     * After calling this method, the server will no longer have any of the previously configured stubs.
     */
    public fun resetAllStubs() {
        mock.resetAll()
    }

    /**
     * Verifies that there are no unmatched requests for the WireMock server.
     *
     * This function checks the WireMock server for any requests that were not matched against
     * any stub mappings. If there are unmatched requests, it will find near misses and throw an
     * appropriate `VerificationException`.
     *
     * @throws VerificationException if there are unmatched requests or near misses.
     */
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
