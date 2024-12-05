package no.nav.arbeidsplassen.stillingshistorikk

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.bigquery.QueryJobConfiguration
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.arbeidsplassen.stillingshistorikk.app.test.TestRunningApplication
import no.nav.arbeidsplassen.stillingshistorikk.nais.HealthService
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class IntegrasjonTest : TestRunningApplication() {
    val topic = "test-topic-${java.util.UUID.randomUUID()}"
    val groupId = "test-group-${java.util.UUID.randomUUID()}"
    val stilling: AdTransport = appCtx.objectMapper.readValue(File("src/test/resources/stilling.json"))
    val table = "${appCtx.bigQuery.options.projectId}.${appCtx.adSchemaTableDefinition.dataSet}.${appCtx.adSchemaTableDefinition.tableName}"
    val healthService = mockk<HealthService>()
    val producer = appCtx.kafkaConfig.kafkaProducer()
    val adTopicListener = AdTopicListener(
        kafkaConsumer = appCtx.kafkaConfig.kafkaJsonConsumer(topic, groupId),
        bigQueryService = appCtx.bigQueryService,
        topic = topic,
        objectMapper = appCtx.objectMapper,
        healthService = healthService
    )

    @AfterEach
    fun ryddOpp() {
        appCtx.bigQuery.query(QueryJobConfiguration.of("DELETE FROM `${table}` WHERE true"))
    }

    @Test
    fun `Skal lese i batcher og ikke legge inn duplikater når den leser inn hyppig innkommende meldinger`() {
        val antallMelinger = 500
        val minimumForventetBatchStørrelse = 6
        every { healthService.isHealthy() } returnsMany List(antallMelinger/minimumForventetBatchStørrelse) { true } + false // sender antall isHealthy()-kall lik maks antall batcher gitt minimumForventetBatchStørrelse, så false for å bryte while-løkken

        val lytter = adTopicListener.startLytter() // starter lytteren og lagrer tråden

        for (i in 0..<antallMelinger) {
            Thread.sleep(10) // venter før den sender neste melding for å simulere innkommende meldinger
            producer.send(ProducerRecord(topic, 0, i.toString(), appCtx.objectMapper.writeValueAsBytes(stilling)))
        }

        lytter.join() // venter på at tråden skal bli ferdig

        val response = appCtx.bigQuery.query(QueryJobConfiguration.of("SELECT * FROM ${table}"))
        verify(exactly = 0) { healthService.addUnhealthyVote() } // sjekker at det ikke ble lagt til noen votes for unhealthy aka ingen exceptions
        verify(atLeast = antallMelinger/minimumForventetBatchStørrelse) { healthService.isHealthy() } // sjekker at isHealthy() ble kalt minst antall batcher gitt minimumForventetBatchStørrelse
        confirmVerified(healthService) // sjekker at alle mocks ble verifisert
        assertThat(response.iterateAll().toList().size).isEqualTo(antallMelinger) // sjekker for duplikater
    }
}