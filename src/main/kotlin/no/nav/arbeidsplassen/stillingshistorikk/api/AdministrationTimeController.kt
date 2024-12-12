package no.nav.arbeidsplassen.stillingshistorikk.api

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import no.nav.arbeidsplassen.stillingshistorikk.BigQueryService
import no.nav.arbeidsplassen.stillingshistorikk.sikkerhet.Rolle
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
        val startTidspunkt = try {
            ctx.queryParam("from")?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) } ?: LocalDate.now().withDayOfMonth(1)
        } catch (e: Exception) {
            ctx.status(HttpStatus.BAD_REQUEST).result("Ugyldig format for parameter 'from'")
            return
        }

        val sluttTidspunkt = try {
            ctx.queryParam("to")?.let { LocalDate.parse(it, DateTimeFormatter.ISO_DATE) } ?: startTidspunkt.plusMonths(1)
        } catch (e: Exception) {
            ctx.status(HttpStatus.BAD_REQUEST).result("Ugyldig format for parameter 'to'")
            return
        }

        LOG.info("Getting report from $startTidspunkt to $sluttTidspunkt")
        val behandlingstid = bigQueryService.queryAdministrationTime(startTidspunkt, sluttTidspunkt)
        ctx.contentType("text/csv").result(behandlingstid)
    }
}