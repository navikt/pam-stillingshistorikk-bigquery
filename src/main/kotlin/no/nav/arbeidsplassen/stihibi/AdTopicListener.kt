package no.nav.arbeidsplassen.stihibi

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsplassen.stihibi.kafka.KafkaListener
import no.nav.arbeidsplassen.stihibi.nais.HealthService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import kotlin.concurrent.thread

class AdTopicListener(
    override val kafkaConsumer: KafkaConsumer<String?, ByteArray?>,
    private val bigQueryService: BigQueryService,
    private val topic: String,
    private val objectMapper: ObjectMapper,
    override val healthService: HealthService,
) : KafkaListener<ByteArray>() {
    companion object {
        private val LOG = LoggerFactory.getLogger(AdTopicListener::class.java)
    }
    override fun startLytter(): Thread {
        return thread(name = "KafkaListener ${AdTopicListener::class.java.name}") { startInternLytter() }
    }

    override fun handleRecords(records: ConsumerRecords<String?, ByteArray?>) {
        try {
//            records.forEach { record ->
//                MDC.put("U", record.key())
//                val eventId = record.headers().headers("@eventId").firstOrNull()?.let { String(it.value()) }
//                MDC.put("TraceId", eventId)
//            }
            val response = bigQueryService.sendBatch(
                ads = records.map { objectMapper.readValue(it.value(), AdTransport::class.java) },
                offsets = records.map { it.offset() },
                partitions = records.map { it.partition() },
                topics = List(records.count()) { topic },
            )

            if (response.hasError) {
                LOG.error("We got error while inserting to bigquery, rows failed {}", response.rowsError)
                LOG.error("failed at start batch offset ${records.first().offset()} partition ${records.first().partition()}")
                throw RowInsertException("Rows inserts failed!")
            }
            LOG.info("Insert successfully, committing offset")
        } finally {
            MDC.clear()
        }
    }
}

class RowInsertException(message: String) : RuntimeException(message)