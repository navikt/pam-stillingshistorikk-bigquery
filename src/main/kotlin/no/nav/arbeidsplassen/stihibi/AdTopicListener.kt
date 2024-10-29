package no.nav.arbeidsplassen.stihibi

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsplassen.stihibi.kafka.KafkaRapidJsonListener
import org.slf4j.LoggerFactory

class AdTopicListener(
    private val bigQueryService: BigQueryService,
    private val topic: String,
    private val objectMapper: ObjectMapper
) : KafkaRapidJsonListener.RapidMessageListener {
    companion object {
        private val LOG = LoggerFactory.getLogger(AdTopicListener::class.java)
    }

    override fun onMessage(message: KafkaRapidJsonListener.JsonMessage) {
        val stilling = objectMapper.readValue(message.payload, AdTransport::class.java)

        val response = bigQueryService.sendBatch(
            ads = listOf(stilling),
            offsets = listOf(message.offset!!),
            partitions = listOf(message.partition!!),
            topics = listOf(topic)
        )
        if (response.hasError) {
            LOG.error("We got error while inserting to bigquery, rows failed {}", response.rowsError)
            LOG.error("failed at start batch offset ${message.offset} partition ${message.partition}")
            throw Throwable("Rows inserts failed!")
        }
        LOG.info("Insert successfully, committing offset")
    }
}