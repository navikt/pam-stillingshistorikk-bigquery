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
class AdTopicListener() {

    companion object {
        private val LOG = LoggerFactory.getLogger(AdTopicListener::class.java)
    }


    @Topic("\${adlistener.topic:StillingIntern}")
    fun receive(ads: List<AdTransport>, offsets: List<Long>, partitions: List<Int>) {
        LOG.info("Received batch with {} ads", ads.size)
        if (ads.isNotEmpty()) {
            LOG.info("push to bigquery here")
        }
     }

}

const val AD_LISTENER_CLIENT_ID="pam-stihibi-ad-topic-listener"
