package me.kpavlov.finchly.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.VerificationException
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

private const val MAX_REQUEST_JOURNAL_ENTRIES = 100

/**
 * Abstract base class for managing a WireMock server instance.
 *
 * Server is started upon initialization
 *
 * @property mock The WireMock server instance.
 */
abstract class BaseWiremock(
    @Suppress("MemberVisibilityCanBePrivate")
    protected val mock: WireMockServer,
) {
    /**
     * Constructs a `BaseWiremock` instance with the specified `WireMockConfiguration`.
     *
     * @param options The configuration settings for initializing the `WireMockServer`.
     */
    @Suppress("unused")
    constructor(options: WireMockConfiguration) : this(WireMockServer(options))

    /**
     * Creates an instance of `BaseWiremock` by configuring a `WireMockServer`.
     *
     * @param block A lambda function that takes a `WireMockConfiguration` object and configures it.
     */
    @Suppress("unused")
    constructor(block: (WireMockConfiguration) -> Unit) : this(
        WireMockServer(
            WireMockConfiguration
                .options()
                .dynamicPort()
                .extensionScanningEnabled(true)
                .maxRequestJournalEntries(MAX_REQUEST_JOURNAL_ENTRIES)
                .also(block),
        ),
    )

    init {
        mock.start()
    }

    /**
     * Retrieves the port number on which the WireMock server instance is running.
     *
     * @return the port number on which the WireMock server instance is listening.
     */
    fun port(): Int = mock.port()

    /**
     * Removes a specific stub mapping from the WireMock server.
     *
     * @param mapping The stub mapping to remove.
     */
    fun resetStub(mapping: com.github.tomakehurst.wiremock.stubbing.StubMapping) {
        mock.removeStub(mapping)
    }

    /**
     * Resets all the stub mappings in the WireMock server instance.
     *
     * This function clears all the stub mappings that have been set up in the WireMock server.
     * After calling this method, the server will no longer have any of the previously configured stubs.
     */
    fun resetAllStubs() {
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
    fun verifyNoUnmatchedRequests() {
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
