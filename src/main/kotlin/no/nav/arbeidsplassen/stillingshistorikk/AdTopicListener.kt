package no.nav.arbeidsplassen.stillingshistorikk

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsplassen.stillingshistorikk.kafka.KafkaListener
import no.nav.arbeidsplassen.stillingshistorikk.nais.HealthService
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
            val response = bigQueryService.sendBatch(
                ads = records.map { objectMapper.readValue(it.value(), AdTransport::class.java) },
                offsets = records.map { it.offset() },
                partitions = records.map { it.partition() },
                topics = List(records.count()) { topic },
            )

            if (response.hasError) {
                LOG.error("Insetting til BigQuery feilet i ${response.rowsError} rader, feilet ved starten av batchen med offset ${records.first().offset()} og partition ${records.first().partition()}")
                throw RowInsertException("Rows inserts failed!")
            }
            MDC.put("Keys", records.mapNotNull { it.key() }.joinToString(", "))
            LOG.info("Sendte ${records.count()} rader til BigQuery")
        } finally {
            MDC.clear()
        }
    }
}

class RowInsertException(message: String) : RuntimeException(message)