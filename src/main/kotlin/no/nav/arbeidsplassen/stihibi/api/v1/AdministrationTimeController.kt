package no.nav.arbeidsplassen.stihibi.api.v1

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import no.nav.arbeidsplassen.stihibi.BigQueryService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Controller("/api/v1/admin/time")
class AdministrationTimeController(private val bigQueryService: BigQueryService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(AdministrationTimeController::class.java)
    }

    @Get("/report.csv")
    fun administrationTime(@QueryValue from: String?, @QueryValue to: String?):String {
        val periodFrom = if (from!=null) LocalDate.parse(from, DateTimeFormatter.ISO_DATE) else LocalDate.now().withDayOfMonth( 1 )
        val periodTo = if (to!=null) LocalDate.parse(to, DateTimeFormatter.ISO_DATE) else periodFrom.plusMonths(1)
        LOG.info("Getting report from $periodFrom to $periodTo");
        return bigQueryService.queryAdministrationTime(periodFrom, periodTo)
    }

}
