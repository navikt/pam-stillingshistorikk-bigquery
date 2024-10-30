package no.nav.arbeidsplassen.stihibi.bigquery

import no.nav.arbeidsplassen.stihibi.AdTransport
import no.nav.arbeidsplassen.stihibi.BigQueryService
import no.nav.arbeidsplassen.stihibi.bigquery.app.test.TestRunningApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BigQueryServiceTest : TestRunningApplication() {

    private val bigQueryService = BigQueryService(appCtx.adSchemaTableDefinition, appCtx.bigQuery, appCtx.objectMapper)
    private val stillinger: List<AdTransport> = appCtx.objectMapper.readValue(
        javaClass.getResourceAsStream("/stihibi-dummy-data.json"),
        appCtx.objectMapper.typeFactory.constructCollectionType(List::class.java, AdTransport::class.java)
    )
    private val nssBehandledeStillinger: List<AdTransport> = appCtx.objectMapper.readValue(
        javaClass.getResourceAsStream("/stillinger-behandlet-av-nss.json"),
        appCtx.objectMapper.typeFactory.constructCollectionType(List::class.java, AdTransport::class.java)
    )

    @Test
    fun `Skal sende stillinger til BigQuery`() {
        val offsets = MutableList<Long>(stillinger.size) { 0 }
        val partitions = MutableList<Int>(stillinger.size) { 0 }
        val topics = MutableList<String>(stillinger.size) { "" }

        val response = bigQueryService.sendBatch(stillinger, offsets, partitions, topics)

        assertThat(stillinger.size).isEqualTo(3)
        assertThat(response.hasError).isFalse()
        assertThat(response.rowsError).isEqualTo(0)
    }

    @Test
    fun `Skal hente alle avviste stillinger siste året`() {
        val response = bigQueryService.queryAvvisning()
        assertThat(response.size).isEqualTo(3)
        assertThat(response.first().reportee).isEqualTo("Marve Almar Fleksnes")
    }

    @Test
    fun `Skal hente historikken til en stilling`() {
        val forventetStilling: AdTransport = stillinger[0]
        val response =
            bigQueryService.queryAdHistory(uuid = forventetStilling.uuid, year = forventetStilling.created.year)

        assertThat(response.size).isEqualTo(1)
        assertThat(response.first()).isEqualTo(forventetStilling)
    }

    @Test
    fun `Skal hente behandlingstid`() {
        val forventetBehandlingstid = "Periode;Kilde;Median;Gjennomsnitt\n2024-01-01 - 2024-12-01;AMEDIA;3600;3600"

        bigQueryService.sendBatch(
            nssBehandledeStillinger,
            MutableList(nssBehandledeStillinger.size) { 0 },
            MutableList(nssBehandledeStillinger.size) { 0 },
            MutableList(nssBehandledeStillinger.size) { "" })

        val response = bigQueryService.queryAdministrationTime(
            LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-01")
        )
        assertThat(response).isEqualTo(forventetBehandlingstid)
    }
}