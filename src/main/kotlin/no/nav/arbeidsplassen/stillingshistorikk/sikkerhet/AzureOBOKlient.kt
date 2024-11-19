package no.nav.arbeidsplassen.stillingshistorikk.sikkerhet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.arbeidsplassen.stillingshistorikk.common.retryTemplate
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*

class AzureOBOKlient(
    val azureUrl: String,
    val clientId: String,
    val clientSecret: String,
    val httpClient: HttpClient
) {
    companion object {
        private val log = LoggerFactory.getLogger(AzureOBOKlient::class.java)
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
    }

    fun veksleInnToken(accessToken: String, scope: String): String {
        val formData = mapOf(
            "grant_type" to "urn:ietf:params:oauth:grant-type:jwt-bearer",
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "scope" to scope,
            "assertion" to accessToken,
            "requested_token_use" to "on_behalf_of",
        )

        val request = HttpRequest.newBuilder()
            .uri(URI(azureUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
            .build()

        val response = retryTemplate(logg = log, requestUrl = azureUrl) {
            httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            )
        }

        if (response.statusCode() >= 300 || response.body() == null) {
            log.error("Greide ikke å veksle inn token ${response.statusCode()} : ${response.body()}")
            throw RuntimeException("unknown error (responseCode=${response.statusCode()}) ved veksling av token")
        }

        val token = objectMapper.readValue(response.body(), ExchangeToken::class.java)
        return token.accessToken
    }


    private fun getFormDataAsString(f: Map<String, String>): String {
        val params = mutableListOf<String>()
        f.forEach { d ->
            val key = URLEncoder.encode(d.key, "UTF-8")
            val value = URLEncoder.encode(d.value, "UTF-8")
            params.add("${key}=${value}")
        }
        return params.joinToString("&")
    }

    data class ExchangeToken(
        @JsonAlias("access_token")
        val accessToken: String,
        @JsonAlias("expires_in")
        val expiresIn: Int,
    )
}
