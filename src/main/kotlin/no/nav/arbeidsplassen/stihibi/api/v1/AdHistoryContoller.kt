package no.nav.arbeidsplassen.stihibi.api.v1

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import no.nav.arbeidsplassen.stihibi.AdTransport
import no.nav.arbeidsplassen.stihibi.BigQueryService
import org.slf4j.LoggerFactory

@Controller("/api/v1/ads/history")
class AdHistoryContoller(private val bigQueryService: BigQueryService) {


    companion object {
        private val LOG = LoggerFactory.getLogger(AdHistoryContoller::class.java)
    }

    @Get("/{uuid}")
    fun retrieveAdHistory(@PathVariable uuid:String, @QueryValue(defaultValue = "2021") year: Int): List<AdTransport> {
        LOG.info("Fetching history for ad: $uuid from year: $year")
        return bigQueryService.queryAdHistory(uuid,year)
    }

}
