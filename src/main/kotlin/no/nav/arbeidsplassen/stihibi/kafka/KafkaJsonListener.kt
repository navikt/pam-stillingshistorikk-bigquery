package no.nav.arbeidsplassen.stihibi.kafka

import no.nav.arbeidsplassen.stihibi.nais.HealthService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.MDC
import kotlin.concurrent.thread

class KafkaJsonListener(
    override val kafkaConsumer: KafkaConsumer<String?, ByteArray?>,
    val messageListener: MessageListener,
    override val healthService: HealthService
) : KafkaListener<ByteArray>() {
    override fun startLytter(): Thread {
        return thread(name = "KafkaListener ${messageListener.javaClass}") { startInternLytter() }
    }

    override fun handleRecords(records: ConsumerRecords<String?, ByteArray?>) {
        try {
            val messages = ArrayList<JsonMessage>()
            records.forEach { record ->
                MDC.put("U", record.key())
                val eventId = record.headers().headers("@eventId").firstOrNull()?.let { String(it.value()) }
                MDC.put("TraceId", eventId)
                messages.add(JsonMessage(
                    key = record.key() ?: "",
                    eventId = eventId,
                    payload = record.value()?.let { String(it) },
                    timestamp = record.timestamp(),
                    partition = record.partition(),
                    offset = record.offset(),
                    kilde = record.headers().headers("@kilde").firstOrNull()?.let { String(it.value()) }
                ))
            }
            messageListener.onMessages(messages)
        } finally {
            MDC.clear()
        }
    }

    interface MessageListener {
        fun onMessages(messages: List<JsonMessage>)
    }

    class JsonMessage(
        val key: String,
        val eventId: String?,
        val payload: String?,
        val timestamp: Long? = null,
        val partition: Int? = null,
        val offset: Long? = null,
        val kilde: String? = null
    )
}
