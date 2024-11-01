package no.nav.arbeidsplassen.stihibi

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsplassen.stihibi.kafka.KafkaJsonListener
import org.slf4j.LoggerFactory

class AdTopicListener(
    private val bigQueryService: BigQueryService,
    private val topic: String,
    private val objectMapper: ObjectMapper
) : KafkaJsonListener.MessageListener {
    companion object {
        private val LOG = LoggerFactory.getLogger(AdTopicListener::class.java)
    }
    override fun onMessages(messages: List<KafkaJsonListener.JsonMessage>) {
        val response = bigQueryService.sendBatch(
            ads = messages.map { objectMapper.readValue(it.payload, AdTransport::class.java) },
            offsets = messages.map { it.offset ?: 0 },
            partitions = messages.map { it.partition ?: 0 },
            topics = List(messages.size) { topic },
        )

        if (response.hasError) {
            LOG.error("We got error while inserting to bigquery, rows failed {}", response.rowsError)
            LOG.error("failed at start batch offset ${messages.first().offset} partition ${messages.first().partition}")
            throw Throwable("Rows inserts failed!")
        }
        LOG.info("Insert successfully, committing offset")
    }
}