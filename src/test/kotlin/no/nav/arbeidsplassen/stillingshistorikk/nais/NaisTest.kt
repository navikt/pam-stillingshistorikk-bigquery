package no.nav.arbeidsplassen.stillingshistorikk.nais

import io.javalin.http.HttpStatus
import no.nav.arbeidsplassen.stillingshistorikk.app.test.TestRunningApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NaisTest : TestRunningApplication() {

    @Test
    fun isAlive() {
        val request =
            HttpRequest.newBuilder().uri(URI("$lokalBaseUrl/internal/isAlive")).GET().build()
        val response = HttpClient.newBuilder().build().send(request, BodyHandlers.ofString())
        assertEquals(HttpStatus.OK.code, response.statusCode())
    }

    @Test
    fun iReady() {
        val request =
            HttpRequest.newBuilder().uri(URI("$lokalBaseUrl/internal/isReady")).GET().build()
        val response = HttpClient.newBuilder().build().send(request, BodyHandlers.ofString())
        assertEquals(HttpStatus.OK.code, response.statusCode())
    }

}
