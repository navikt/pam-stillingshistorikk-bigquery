package no.nav.arbeidsplassen.stihibi.api.v1

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.Javalin
import io.javalin.http.Context
import no.nav.arbeidsplassen.stihibi.BigQueryService
import no.nav.arbeidsplassen.stihibi.sikkerhet.Rolle
import org.slf4j.LoggerFactory

class AdHistoryContoller(
    private val bigQueryService: BigQueryService,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AdHistoryContoller::class.java)
    }

    fun setupRoutes(javalin: Javalin) {
        javalin.get("/api/v1/ads/history/{uuid}", { hentStillingsHistorikk(it) }, Rolle.UNPROTECTED)
    }

    private fun hentStillingsHistorikk(ctx: Context) {
        val uuid = ctx.pathParam("uuid")
        val år = ctx.queryParam("year")!!.toInt()
        LOG.info("Fetching history for ad: $uuid from year: $år")
        ctx.result(objectMapper.writeValueAsString(bigQueryService.queryAdHistory(uuid, år)))
    }
}