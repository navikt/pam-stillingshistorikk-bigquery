package no.nav.arbeidsplassen.stillingshistorikk.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import no.nav.arbeidsplassen.stillingshistorikk.BigQueryService
import no.nav.arbeidsplassen.stillingshistorikk.sikkerhet.Rolle
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
        val år = ctx.queryParam("year")?.toInt()
        if (år == null) {
            ctx.status(HttpStatus.BAD_REQUEST).contentType("text/plain").result("Mangler parameter 'year'")
        } else {
            ctx.result(objectMapper.writeValueAsString(bigQueryService.queryAdHistory(uuid, år)))
            LOG.info("Henter stillingshistorikk for stilling: $uuid og år: $år")
        }
    }
}