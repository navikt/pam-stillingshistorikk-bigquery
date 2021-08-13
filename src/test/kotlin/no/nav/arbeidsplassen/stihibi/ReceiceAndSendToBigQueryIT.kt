package no.nav.arbeidsplassen.stihibi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.micronaut.context.annotation.Property
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ReceiceAndSendToBigQueryIT {

    private val props: Map<String, String> = hashMapOf(Pair("bootstrap.servers", "localhost:9092"))
    private val kafkaProducer: Producer<String, String> = KafkaProducer<String, String>(props,
        StringSerializer(), StringSerializer()
    )
    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule())
        .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(ReceiceAndSendToBigQueryIT::class.java)
    }

    @Test
    @Timeout(10)
    @Property(name = "indexer.scheduler.enabled", value = "false")
    fun kafkaListenerTest() {
        val adTransport = objectMapper.readValue(
            ReceiceAndSendToBigQueryIT::class.java.getResourceAsStream("/adto/fullAdDTO.json"),
            AdTransport::class.java
        )
        kafkaProducer.send(ProducerRecord("StillingIntern", "10584075-1e5c-4a83-bf49-c80bcab83018", objectMapper.writeValueAsString(adTransport)))
        kafkaProducer.flush()
        LOG.info("Ad sent")
        Thread.sleep(3000)
    }
}
