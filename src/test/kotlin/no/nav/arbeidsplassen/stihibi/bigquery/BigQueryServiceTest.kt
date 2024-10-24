package no.nav.arbeidsplassen.stihibi.bigquery

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.*
import no.nav.arbeidsplassen.stihibi.AdSchemaTableDefinition
import no.nav.arbeidsplassen.stihibi.AdTransport
import no.nav.arbeidsplassen.stihibi.BigQueryService
import no.nav.arbeidsplassen.stihibi.bigquery.app.env
import no.nav.arbeidsplassen.stihibi.bigquery.app.test.TestRunningApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BigQueryServiceTest : TestRunningApplication() {

    private lateinit var adSchemaTableDefinition: AdSchemaTableDefinition
    private lateinit var bigQuery: BigQuery
    private lateinit var bigQueryService: BigQueryService
    private val testData = javaClass.getResourceAsStream("/stihibi-dummy-data.json")
    private val stillingerJSON = appCtx.objectMapper.readValue(testData, JsonNode::class.java)

    @BeforeAll
    fun init() {
        adSchemaTableDefinition = AdSchemaTableDefinition(appCtx.objectMapper)
        bigQuery = BigQueryOptions.newBuilder()
            .setProjectId(env.getValue("BIGQUERY_PROJECT_ID"))
            .setHost(env.getValue("BIGQUERY_ENDPOINT"))
            .setLocation(env.getValue("BIGQUERY_ENDPOINT"))
            .setCredentials(NoCredentials.getInstance())
            .build()
            .service.also { bigQuery ->
                // Oppretter dataset for den lokale instansen, BigQueryService lager tabellen selv
                bigQuery.create(DatasetInfo.of(adSchemaTableDefinition.dataSet))
            }
        bigQueryService = BigQueryService(adSchemaTableDefinition, bigQuery, appCtx.objectMapper)
    }

    @Test
    fun `Skal sende stillinger til BigQuery`() {
        val stillinger = ArrayList<AdTransport>()
        stillingerJSON.map { rad ->
            stillinger.add(appCtx.objectMapper.readValue(rad.get("json").asText(), AdTransport::class.java))
        }

        val offsets = MutableList<Long>(stillinger.size) { 0 }
        val partitions = MutableList<Int>(stillinger.size) { 0 }
        val topics = MutableList<String>(stillinger.size) { "" }

        val response = bigQueryService.sendBatch(stillinger, offsets, partitions, topics)

        assertThat(stillinger.size).isEqualTo(20)
        assertThat(response.hasError).isFalse()
        assertThat(response.rowsError).isEqualTo(0)
    }

    @Test
    fun `Skal hente alle avviste stillinger siste året`() {
        val response = bigQueryService.queryAvvisning()
        assertThat(response.size).isEqualTo(10)
        assertThat(response.first().reportee).isEqualTo("Marve Almar Fleksnes")
    }

    @Test
    fun `Skal hente historikken til en stilling`() {
        val forventetStilling: AdTransport = appCtx.objectMapper.readValue(stillingerJSON[0].get("json").asText())
        val response = bigQueryService.queryAdHistory(uuid = forventetStilling.uuid, year = forventetStilling.created.year)

        assertThat(response.size).isEqualTo(1)
        assertThat(response.first()).isEqualTo(forventetStilling)
    }

    @Test
    @Disabled
    fun `Skal hente behandlingstid`() {
        TODO()
    }
}