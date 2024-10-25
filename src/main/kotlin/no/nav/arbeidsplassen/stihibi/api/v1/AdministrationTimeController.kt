package no.nav.arbeidsplassen.stihibi.api.v1

import io.javalin.Javalin
import io.javalin.http.Context
import no.nav.arbeidsplassen.stihibi.BigQueryService
import no.nav.arbeidsplassen.stihibi.sikkerhet.Rolle
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AdministrationTimeController(
    private val bigQueryService: BigQueryService
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AdministrationTimeController::class.java)
    }

    fun setupRoutes(javalin: Javalin) {
        javalin.get("/api/v1/admin/report/behandlingstid.csv", { hentBehandlingstid(it) }, Rolle.UNPROTECTED)
    }

    private fun hentBehandlingstid(ctx: Context) {
        val startTidspunkt = if (ctx.queryParam("from") == null) LocalDate.now().withDayOfMonth(1) else
            LocalDate.parse(ctx.queryParam("from")!!, DateTimeFormatter.ISO_DATE)
        val sluttTidspunkt = if (ctx.queryParam("to") == null) startTidspunkt.plusMonths(1) else
            LocalDate.parse(ctx.queryParam("to")!!, DateTimeFormatter.ISO_DATE)

        LOG.info("Getting report from $startTidspunkt to $sluttTidspunkt");
        val behandlingstid = bigQueryService.queryAdministrationTime(startTidspunkt, sluttTidspunkt)
        ctx.contentType("text/csv").result(behandlingstid)
    }
}