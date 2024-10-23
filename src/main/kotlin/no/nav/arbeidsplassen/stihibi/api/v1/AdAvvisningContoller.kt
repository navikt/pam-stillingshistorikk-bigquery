package no.nav.arbeidsplassen.stihibi.api.v1

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.Javalin
import io.javalin.http.Context
import no.nav.arbeidsplassen.stihibi.BigQueryService
import no.nav.arbeidsplassen.stihibi.sikkerhet.Rolle
import org.slf4j.LoggerFactory

class AdAvvisningContoller(
    private val bigQueryService: BigQueryService,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AdAvvisningContoller::class.java)
    }

    fun setupRoutes(javalin: Javalin) {
        javalin.get("/api/v1/ads/avvisning", {hentAvvisteStillinger(it)}, Rolle.UNPROTECTED)
        javalin.exception(Exception::class.java) { e, ctx -> håndterFeilmelding(e, ctx) }
    }

    fun hentAvvisteStillinger(ctx: Context) {
        LOG.info("Henter alle avvisninger det siste året")
        ctx.result(objectMapper.writeValueAsString(bigQueryService.queryAvvisning()))
    }

    fun håndterFeilmelding(e: Exception, ctx: Context) {
        LOG.error("Error handling request ${ctx.url()}", e)
        TODO("Håndter feil")
    }
}