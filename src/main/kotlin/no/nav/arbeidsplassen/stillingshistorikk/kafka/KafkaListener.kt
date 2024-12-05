package no.nav.arbeidsplassen.stillingshistorikk.kafka

import no.nav.arbeidsplassen.stillingshistorikk.RowInsertException
import no.nav.arbeidsplassen.stillingshistorikk.nais.HealthService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.errors.AuthorizationException
import org.apache.kafka.common.errors.SerializationException
import org.slf4j.LoggerFactory
import java.time.Duration
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
        LOG.info("Starter Kafka stillingshistorikk lytter")
        var records: ConsumerRecords<String?, T?>?
        while (healthService.isHealthy()) {
            try {
                records = kafkaConsumer.poll(Duration.ofSeconds(1))
                if (records.count() > 0) {
                    LOG.info("Leste ${records.count()} rader. Keys: {}", records.mapNotNull { it.key() }.joinToString())
                    handleRecords(records)
                    kafkaConsumer.commitSync()
                    LOG.info("Committing offset")
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
            }
        }
        kafkaConsumer.close()
    }

    fun pauseLytter() = kontrollKø.put(KafkaState.PAUSE)
    fun gjenopptaLytter() = kontrollKø.put(KafkaState.GJENOPPTA)
    fun harFeilet() = healthService.isHealthy().not()

    enum class KafkaState {
        PAUSE, GJENOPPTA
    }
}