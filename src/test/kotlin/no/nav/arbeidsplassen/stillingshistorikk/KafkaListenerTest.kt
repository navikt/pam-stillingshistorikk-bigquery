package no.nav.arbeidsplassen.stillingshistorikk

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.arbeidsplassen.stillingshistorikk.app.test.TestRunningApplication
import no.nav.arbeidsplassen.stillingshistorikk.kafka.KafkaListener
import no.nav.arbeidsplassen.stillingshistorikk.nais.HealthService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.AuthorizationException
import org.apache.kafka.common.errors.SerializationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.*
import kotlin.concurrent.thread

private val recordCollector = mutableListOf<ConsumerRecords<String?, ByteArray?>>()

private class KafkaListenerTester(
    override val kafkaConsumer: KafkaConsumer<String?, ByteArray?>,
    override val healthService: HealthService,
) : KafkaListener<ByteArray>() {
    override fun startLytter() = thread(name = "KafkaListenerTester") { startInternLytter() }
    override fun handleRecords(records: ConsumerRecords<String?, ByteArray?>) {
        recordCollector.add(records)
    }
}

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class KafkaListenerTest : TestRunningApplication() {
    private val topic = "test-topic-${UUID.randomUUID()}"
    private val healthService = mockk<HealthService>()
    private val kafkaConsumer = mockk<KafkaConsumer<String?, ByteArray?>>()
    private val produsent = appCtx.kafkaConfig.kafkaProducer()
    private val kafkaListener = KafkaListenerTester(
        kafkaConsumer = kafkaConsumer,
        healthService = healthService
    )

    @AfterEach
    fun ryddOpp() {
        clearMocks(healthService)
        recordCollector.clear()
    }

    @Test
    fun `Skal starte lytter uten feil`() {
        every { healthService.isHealthy() } returns true andThen false // true ved første kall, false ved andre for å bryte while-løkken
        val exeption = catchThrowable { kafkaListener.startLytter().join() } // join for å vente på at tråden er ferdig

        assert(exeption == null)
    }

    @Test
    fun `Skal kunne lese inn meldinger`() {
        val melding = ProducerRecord(topic, "", "value".toByteArray())
        val forventetMelding = ConsumerRecord(topic, 0, 0, "", "value".toByteArray())
        produsent.send(melding)

        every { healthService.isHealthy() } returns true andThen false
        every { kafkaConsumer.poll(Duration.ofSeconds(1)) } answers {
            ConsumerRecords(mapOf(Pair(TopicPartition(topic, 0), listOf(forventetMelding))))
        }
        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        assertThat(exeption).isNull()
        assertThat(recordCollector.size).isEqualTo(1)
        assertThat(recordCollector.first().count()).isEqualTo(1)
        assertThat(recordCollector.first().first()).isEqualTo(forventetMelding)
    }

    @Test
    fun `Skal håntere AuthorizationException i consumerloop og øke antall unhealthy votes`() {
        every { healthService.isHealthy() } returns true andThen false
        every { healthService.addUnhealthyVote() } returns 1
        every { kafkaConsumer.poll(Duration.ofSeconds(10)) } throws AuthorizationException("AuthorizationException")

        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        verify(exactly = 1) { healthService.addUnhealthyVote() }
        assertThat(exeption).isNull()
    }

    @Test
    fun `Skal håntere KafkaException i consumerloop og øke antall unhealthy votes`() {
        every { healthService.isHealthy() } returns true andThen false
        every { healthService.addUnhealthyVote() } returns 1
        every { kafkaConsumer.poll(Duration.ofSeconds(10)) } throws KafkaException("KafkaException")

        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        verify(exactly = 1) { healthService.addUnhealthyVote() }
        assertThat(exeption).isNull()
    }

    @Test
    fun `Skal håntere SerializationException i consumerloop og øke antall unhealthy votes`() {
        every { healthService.isHealthy() } returns true andThen false
        every { healthService.addUnhealthyVote() } returns 1
        every { kafkaConsumer.poll(Duration.ofSeconds(10)) } throws SerializationException("SerializationException")

        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        verify(exactly = 1) { healthService.addUnhealthyVote() }
        assertThat(exeption).isNull()
    }

    @Test
    fun `Skal håntere RowInsertException i consumerloop og øke antall unhealthy votes`() {
        every { healthService.isHealthy() } returns true andThen false
        every { healthService.addUnhealthyVote() } returns 1
        every { kafkaConsumer.poll(Duration.ofSeconds(10)) } throws RowInsertException("RowInsertException")

        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        verify(exactly = 1) { healthService.addUnhealthyVote() }
        assertThat(exeption).isNull()
    }

    @Test
    fun `Skal håntere uventet Exception i consumerloop og øke antall unhealthy votes`() {
        every { healthService.isHealthy() } returns true andThen false
        every { healthService.addUnhealthyVote() } returns 1
        every { kafkaConsumer.poll(Duration.ofSeconds(10)) } throws Exception("Exception")

        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        verify(exactly = 1) { healthService.addUnhealthyVote() }
        assertThat(exeption).isNull()
    }
}