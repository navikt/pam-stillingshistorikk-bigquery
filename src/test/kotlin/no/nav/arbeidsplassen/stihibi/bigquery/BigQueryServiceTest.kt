package no.nav.arbeidsplassen.stihibi.bigquery

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.bigquery.QueryJobConfiguration
import no.nav.arbeidsplassen.stihibi.AdTransport
import no.nav.arbeidsplassen.stihibi.Avvisning
import no.nav.arbeidsplassen.stihibi.BigQueryService
import no.nav.arbeidsplassen.stihibi.bigquery.app.test.TestRunningApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class BigQueryServiceTest : TestRunningApplication() {
    private inline fun <reified T>readFile(filnavn: String) = appCtx.objectMapper.readValue<T>(File(filnavn))
    private val tableFNAME = "${appCtx.bigQuery.options.projectId}.${appCtx.adSchemaTableDefinition.dataSet}.${appCtx.adSchemaTableDefinition.tableNameV1}"
    private val stilling: AdTransport = readFile("src/test/resources/stilling.json")
    private val stillinger: List<AdTransport> = readFile("src/test/resources/stihibi-dummy-data.json")
    private val nssBehandledeStillinger: List<AdTransport> = readFile("src/test/resources/stillinger-behandlet-av-nss.json")
    private val bigQueryService = BigQueryService(appCtx.adSchemaTableDefinition, appCtx.bigQuery, appCtx.objectMapper)

    @AfterEach
    fun ryddOpp() {
        appCtx.bigQuery.query(QueryJobConfiguration.of("DELETE FROM `${tableFNAME}` WHERE true"))
    }

    @Test
    fun `Skal sende stillinger til BigQuery`() {
        val response = bigQueryService.sendBatch(
            stillinger,
            List(3) { 0L },
            List(3) { 0 },
            List(3) { "" })

        assertThat(stillinger.size).isEqualTo(3)
        assertThat(response.hasError).isFalse()
        assertThat(response.rowsError).isEqualTo(0)
    }

    @Test
    fun `Skal hente alle avviste stillinger siste året`() {
        val sendteStillinger = stillinger + nssBehandledeStillinger
        val avvisteStillinger: List<Avvisning> = readFile("src/test/resources/avviste-stillinger.json")
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