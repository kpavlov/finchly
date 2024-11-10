package me.kpavlov.finchly.wiremock

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.messageContains
import assertk.fail
import com.github.tomakehurst.wiremock.client.VerificationException
import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class BaseWiremockTest {
    private val mock =
        object : BaseWiremock({}) {
            fun shouldSayHello() {
                mock.stubFor(
                    WireMock
                        .get("/hello")
                        .willReturn(
                            WireMock.ok("Hello"),
                        ),
                )
            }
        }

    private val client = HttpClient.newHttpClient()
    private val port = mock.port()

    @Test
    fun `Should mock method`() {
        // given
        mock.shouldSayHello()
        // when
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/hello"))
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // then
        assertThat(response.body()).isEqualTo("Hello")
    }

    @Test
    fun `Should catch unmatched request`() {
        // when
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port/bye"))
                .build()

        client.send(request, HttpResponse.BodyHandlers.ofString())

        // then
        try {
            mock.verifyNoUnmatchedRequests()
            fail("Should catch unmatched request")
        } catch (e: VerificationException) {
            println(e)
            assertThat(e).messageContains("Unmatched requests found")
            assertThat(e).messageContains("\"url\" : \"/bye\"")
        }
    }
}
