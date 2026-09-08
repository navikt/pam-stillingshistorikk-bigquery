package no.nav.arbeidsplassen.stillingshistorikk

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.bigquery.QueryJobConfiguration
import no.nav.arbeidsplassen.stillingshistorikk.app.test.TestRunningApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class BigQueryServiceTest : TestRunningApplication() {
    private inline fun <reified T>readFile(filnavn: String) = appCtx.objectMapper.readValue<T>(File(filnavn))
    private val tableFNAME = "${appCtx.bigQuery.options.projectId}.${appCtx.adSchemaTableDefinition.dataSet}.${appCtx.adSchemaTableDefinition.tableName}"
    private val stilling: AdTransport = readFile("src/test/resources/stilling.json")
    private val stillinger: List<AdTransport> = readFile("src/test/resources/stillingshistorikk-dummy-data.json")
    private val nssBehandledeStillinger: List<AdTransport> = readFile("src/test/resources/stillinger-behandlet-av-nss.json")
    private val bigQueryService = BigQueryService(appCtx.adSchemaTableDefinition, appCtx.bigQuery, appCtx.objectMapper)

    @AfterEach
    fun ryddOpp() {
        appCtx.bigQuery.query(QueryJobConfiguration.of("DELETE FROM `${tableFNAME}` WHERE true"))
    }

    @Test
    fun `Skal sende stillinger til BigQuery`() {
        val antallStillinger = 100
        val response = bigQueryService.sendBatch(
            List(antallStillinger) { stilling },
            List(antallStillinger) { 0L },
            List(antallStillinger) { 0 },
            List(antallStillinger) { "" })
        val mottatteRader = appCtx.bigQuery.query(QueryJobConfiguration.of("SELECT * FROM `${tableFNAME}`"))

        assertThat(mottatteRader.iterateAll().count()).isEqualTo(antallStillinger)
        assertThat(response.hasError).isFalse()
        assertThat(response.rowsError).isEqualTo(0)
    }

    @Test
    fun `Skal hente alle avviste stillinger siste året`() {
        // Testdataene har faste timestamps fra 2024. queryAvvisning filtrerer på "created" siste året
        // fra CURRENT_DATETIME(), så vi forskyver tidsstemplene relativt til dagens dato for at testen
        // ikke skal feile når den kjøres mer enn ett år etter at testdataene ble skrevet.
        val forskyvning = Duration.between(LocalDateTime.parse("2024-05-08T06:00:50.306669"), LocalDateTime.now().minusMonths(6))
        val sendteStillinger = (stillinger + nssBehandledeStillinger).map {
            it.copy(created = it.created.plus(forskyvning), updated = it.updated.plus(forskyvning))
        }
        val avvisteStillinger: List<Avvisning> = readFile<List<Avvisning>>("src/test/resources/avviste-stillinger.json")
            .map { it.copy(avvist_tidspunkt = it.avvist_tidspunkt.plus(forskyvning)) }
        bigQueryService.sendBatch(
            sendteStillinger,
            List(sendteStillinger.size) { 0L },
            List(sendteStillinger.size) { 0 },
            List(sendteStillinger.size) { "" })

        val response: List<Avvisning> = bigQueryService.queryAvvisning()
        assertThat(response).isEqualTo(avvisteStillinger)
    }

    @Test
    fun `Skal hente historikken til en stilling`() {
        bigQueryService.sendBatch(listOf(stilling), List(1) { 0L }, List(1) { 0 }, List(1) { "" })
        val response = bigQueryService.queryAdHistory(uuid = stilling.uuid, year = stilling.created.year)

        assertThat(response.size).isEqualTo(1)
        assertThat(response.first()).isEqualTo(stilling)
    }

    @Test
    fun `Skal hente behandlingstid`() {
        val forventetBehandlingstid = "Periode;Kilde;Median;Gjennomsnitt\n2024-01-01 - 2024-12-01;AMEDIA;3600;3600"

        bigQueryService.sendBatch(
            nssBehandledeStillinger,
            List(nssBehandledeStillinger.size) { 0L },
            List(nssBehandledeStillinger.size) { 0 },
            List(nssBehandledeStillinger.size) { "" })

        val response = bigQueryService.queryAdministrationTime(
            LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-01")
        )
        assertThat(response).isEqualTo(forventetBehandlingstid)
    }
}