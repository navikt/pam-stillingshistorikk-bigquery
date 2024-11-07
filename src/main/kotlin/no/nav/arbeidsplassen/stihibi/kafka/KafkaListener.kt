package no.nav.arbeidsplassen.stihibi.kafka

import no.nav.arbeidsplassen.stihibi.RowInsertException
import no.nav.arbeidsplassen.stihibi.nais.HealthService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.AuthorizationException
import org.apache.kafka.common.errors.SerializationException
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

abstract class KafkaListener<T> {
    companion object {
        private val LOG = LoggerFactory.getLogger(KafkaListener::class.java)
    }

    abstract val healthService: HealthService
    abstract val kafkaConsumer: KafkaConsumer<String?, T?>

    abstract fun startLytter(): Thread
    abstract fun handleRecords(records: ConsumerRecords<String?, T?>): Unit?

    val kontrollKø: BlockingQueue<KafkaState> = LinkedBlockingQueue()

    fun startInternLytter() {
        LOG.info("Starter Kafka stihibi lytter")
        var records: ConsumerRecords<String?, T?>?
        while (healthService.isHealthy()) {
            var currentPositions = mutableMapOf<TopicPartition, Long>()
            val setStatus = kontrollKø.poll()
            try {
                if (setStatus != null)
                    when (setStatus) {
                        KafkaState.PAUSE -> {
                            kafkaConsumer.pause(currentPositions.keys)
                            LOG.info("Pauset Kafka stihibi lytter")
                        }
                        KafkaState.GJENOPPTA -> {
                            kafkaConsumer.resume(currentPositions.keys)
                            LOG.info("Gjenopptar Kafka stihibi lytter")
                        }
                    }
                records = kafkaConsumer.poll(Duration.ofSeconds(10))
                LOG.info("Poller Kafka stihibi lytter, antall rader: ${records.count()}")
                if (records.count() > 0) {
                    currentPositions = records
                        .groupBy { TopicPartition(it.topic(), it.partition()) }
                        .mapValues { it.value.minOf { it.offset() } }
                        .toMutableMap()

                    LOG.info("Leste ${records.count()} rader. Keys: {}", records.mapNotNull { it.key() }.joinToString())
                    handleRecords(records)
                }
            } catch (e: AuthorizationException) {
                LOG.error("AuthorizationException i consumerloop, restarter app ${e.message}", e)
                healthService.addUnhealthyVote()
            } catch (e: SerializationException) {
                LOG.error("SerializationException i consumerloop, restarter app ${e.message}", e)
                healthService.addUnhealthyVote()
            } catch (ke: KafkaException) {
                LOG.error("KafkaException i consumerloop, restarter app ${ke.message}", ke)
                healthService.addUnhealthyVote()
            } catch (e: RowInsertException) {
                LOG.error("RowInsertException i consumerloop, restarter app ${e.message}", e)
                healthService.addUnhealthyVote()
            } catch (e: Exception) {
                // Catchall - impliserer at vi skal restarte app
                LOG.error("Uventet Exception i consumerloop, restarter app ${e.message}", e)
                healthService.addUnhealthyVote()
            } finally {
                kafkaConsumer.commitSync(currentPositions.mapValues { (_, offset) -> offsetMetadata(offset) })
                currentPositions.clear()
            }
        }
        kafkaConsumer.close()
    }

    fun pauseLytter() = kontrollKø.put(KafkaState.PAUSE)
    fun gjenopptaLytter() = kontrollKø.put(KafkaState.GJENOPPTA)
    fun harFeilet() = healthService.isHealthy().not()

    private fun offsetMetadata(offset: Long): OffsetAndMetadata {
        val clientId = kafkaConsumer.groupMetadata().groupInstanceId().map { "\"$it\"" }.orElse("null")

        @Language("JSON")
        val metadata = """{"time": "${LocalDateTime.now()}","groupInstanceId": $clientId}"""
        return OffsetAndMetadata(offset, metadata)
    }

    enum class KafkaState {
        PAUSE, GJENOPPTA
    }
}