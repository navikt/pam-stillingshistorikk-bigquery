package no.nav.arbeidsplassen.stihibi.bigquery

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.arbeidsplassen.stihibi.BigQueryService
import org.junit.jupiter.api.Test
import java.time.LocalDate

// useful to test against bigquery.
@MicronautTest
class QueryAdministrationTimeIT(private val bigQueryService: BigQueryService) {

    @Test
    fun administrationTime() {
        println(bigQueryService.queryAdministrationTime(LocalDate.of(2021, 9, 1), LocalDate.of(2021,10,1)))
    }
}
