package no.nav.arbeidsplassen.stihibi

import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetReset
import io.micronaut.configuration.kafka.annotation.OffsetStrategy
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory

@KafkaListener(clientId = AD_LISTENER_CLIENT_ID, groupId = "\${adlistener.group-id:pam-stihibi}", threads = 1, offsetReset = OffsetReset.EARLIEST,
        batch = true, offsetStrategy = OffsetStrategy.SYNC)
@Requires(property = "adlistener.enabled", value = "true")
class AdTopicListener(private val bigQueryService: BigQueryService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(AdTopicListener::class.java)
    }


    @Topic("\${adlistener.topic:StillingIntern}")
    fun receive(ads: List<AdTransport>, offsets: List<Long>, partitions: List<Int>, topic: String) {
        LOG.info("Received batch with {} ads", ads.size)
        if (ads.isNotEmpty()) {
            if (ads.size!=offsets.size || ads.size!=partitions.size)
                LOG.error("Something is not correct, size should be the same")

            val response = bigQueryService.sendBatch(ads, offsets, partitions, "teampam.stilling-intern-1")
            if (response.hasError) {
                LOG.error("We got error while inserting to bigquery, rows failed {}", response.rowsError)
                throw Exception("Rows inserts failed!")
            }
        }
     }
}

const val AD_LISTENER_CLIENT_ID="pam-stihibi-ad-topic-listener"
