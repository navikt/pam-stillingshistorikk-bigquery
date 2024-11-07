package no.nav.arbeidsplassen.stihibi.bigquery

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.*
import no.nav.arbeidsplassen.stihibi.*
import no.nav.arbeidsplassen.stihibi.bigquery.app.test.TestRunningApplication
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class AdTopicListenerTest : TestRunningApplication() {
    private val topic = appCtx.env.getValue("STILLING-HISTORIKK_TOPIC")
    private val stilling: AdTransport = appCtx.objectMapper.readValue(File("src/test/resources/stilling.json"))
    private val bigQueryService: BigQueryService = mockk()
    private val adTopicListener = AdTopicListener(
        kafkaConsumer = appCtx.kafkaConfig.kafkaJsonConsumer(topic, appCtx.env.getValue("STIHIBI_GROUP_ID")),
        bigQueryService = bigQueryService,
        topic = topic,
        objectMapper = appCtx.objectMapper,
        healthService = appCtx.healthService
    )

    @AfterEach
    fun ryddOpp() {
        clearMocks(bigQueryService)
    }

    @Test
    fun `Skal hente inn en batch med meldinger og kalle på BigQuery én gang`() {
        val records = ConsumerRecords<String?, ByteArray?>(mapOf(
            TopicPartition(topic, 0) to List(10) {
                ConsumerRecord(topic, 0, 0L, null, appCtx.objectMapper.writeValueAsBytes(stilling))
            }
        ))
        every { bigQueryService.sendBatch(any(), any(), any(), any()) } returns BigQueryResponse(false, 0)

        adTopicListener.handleRecords(records)

        assertThat(records.count()).isEqualTo(10)
        verify(exactly = 1) { bigQueryService.sendBatch(any(), any(), any(), any()) }
    }

    @Test
    fun `Skal feile ved feil fra BigQuery`() {
        val records = ConsumerRecords<String?, ByteArray?>(mapOf(
            TopicPartition(topic, 0) to List(10) {
                ConsumerRecord(topic, 0, 0L, null, appCtx.objectMapper.writeValueAsBytes(stilling))
            }
        ))
        every { bigQueryService.sendBatch(any(), any(), any(), any()) } returns BigQueryResponse(true, 10)

        val exception = catchThrowable { adTopicListener.handleRecords(records) }

        assertThat(exception).isInstanceOf(RowInsertException::class.java)
        verify(exactly = 1) { bigQueryService.sendBatch(any(), any(), any(), any()) }
    }
}