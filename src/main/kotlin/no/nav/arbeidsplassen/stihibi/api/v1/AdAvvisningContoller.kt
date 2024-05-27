package no.nav.arbeidsplassen.stihibi.api.v1

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Error
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import no.nav.arbeidsplassen.stihibi.Avvisning
import no.nav.arbeidsplassen.stihibi.BigQueryService
import org.slf4j.LoggerFactory

@Controller("/api/v1/ads/avvisning")
class AdAvvisningContoller(private val bigQueryService: BigQueryService) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AdAvvisningContoller::class.java)
    }

    @Get
    fun retrieveAdHistory(): List<Avvisning> {
        LOG.info("Henter alle avvisninger det siste året")
        return bigQueryService.queryAvvisning()
    }

    @Error
    fun handleError(request: HttpRequest<*>, e: Exception): HttpResponse<String> {
        LOG.error("Error handling request ${request.uri}", e)
        return HttpResponse.serverError("An error occurred while processing your request.")
    }
}
