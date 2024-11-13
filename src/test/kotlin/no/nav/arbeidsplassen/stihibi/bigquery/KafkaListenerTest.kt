package no.nav.arbeidsplassen.stihibi.bigquery

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.arbeidsplassen.stihibi.RowInsertException
import no.nav.arbeidsplassen.stihibi.bigquery.app.test.TestRunningApplication
import no.nav.arbeidsplassen.stihibi.kafka.KafkaListener
import no.nav.arbeidsplassen.stihibi.kafka.KafkaListener.KafkaState
import no.nav.arbeidsplassen.stihibi.nais.HealthService
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
    private val topic = appCtx.env.getValue("ADLISTENER_TOPIC")
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
        every { kafkaConsumer.poll(Duration.ofSeconds(10)) } answers {
            ConsumerRecords(mapOf(Pair(TopicPartition(topic, 0), listOf(forventetMelding))))
        }
        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        assertThat(exeption).isNull()
        assertThat(recordCollector.size).isEqualTo(1)
        assertThat(recordCollector.first().count()).isEqualTo(1)
        assertThat(recordCollector.first().first()).isEqualTo(forventetMelding)
    }

    @Test
    fun `Skal kunne pause og gjenoppta lytteren`() {
        every { healthService.isHealthy() } returns true
        every { kafkaConsumer.pause(any()) } returns Unit
        every { kafkaConsumer.resume(any()) } returns Unit
        kafkaListener.kontrollKø.put(KafkaState.GJENOPPTA) // starter i GJENOPPTA for at status ikke skal være null

        kafkaListener.startLytter()
        Thread.sleep(1000) // venter litt for å la lytteren starte

        kafkaListener.pauseLytter()
        Thread.sleep(1000) // venter litt for å la lytteren pause
        assertThat(kafkaListener.kontrollKø.take()).isEqualTo(KafkaState.PAUSE)

        kafkaListener.gjenopptaLytter()
        Thread.sleep(1000) // venter litt for å la lytteren gjenoppta
        assertThat(kafkaListener.kontrollKø.take()).isEqualTo(KafkaState.GJENOPPTA)
    }

    @Test
    fun `Skal håntere AuthorizationException i consumerloop og øke antall unhealthy votes`() {
        every { healthService.isHealthy() } returns true
        every { healthService.addUnhealthyVote() } returns 1
        every { kafkaConsumer.poll(Duration.ofSeconds(10)) } throws AuthorizationException("AuthorizationException")

        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        verify(exactly = 1) { healthService.addUnhealthyVote() }
        assertThat(exeption).isNull()
    }

    @Test
    fun `Skal håntere KafkaException i consumerloop og øke antall unhealthy votes`() {
        every { healthService.isHealthy() } returns true
        every { healthService.addUnhealthyVote() } returns 1
        every { kafkaConsumer.poll(Duration.ofSeconds(10)) } throws KafkaException("KafkaException")

        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        verify(exactly = 1) { healthService.addUnhealthyVote() }
        assertThat(exeption).isNull()
    }

    @Test
    fun `Skal håntere SerializationException i consumerloop og øke antall unhealthy votes`() {
        every { healthService.isHealthy() } returns true
        every { healthService.addUnhealthyVote() } returns 1
        every { kafkaConsumer.poll(Duration.ofSeconds(10)) } throws SerializationException("SerializationException")

        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        verify(exactly = 1) { healthService.addUnhealthyVote() }
        assertThat(exeption).isNull()
    }

    @Test
    fun `Skal håntere RowInsertException i consumerloop og øke antall unhealthy votes`() {
        every { healthService.isHealthy() } returns true
        every { healthService.addUnhealthyVote() } returns 1
        every { kafkaConsumer.poll(Duration.ofSeconds(10)) } throws RowInsertException("RowInsertException")

        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        verify(exactly = 1) { healthService.addUnhealthyVote() }
        assertThat(exeption).isNull()
    }

    @Test
    fun `Skal håntere uventet Exception i consumerloop og øke antall unhealthy votes`() {
        every { healthService.isHealthy() } returns true
        every { healthService.addUnhealthyVote() } returns 1
        every { kafkaConsumer.poll(Duration.ofSeconds(10)) } throws Exception("Exception")

        val exeption = catchThrowable { kafkaListener.startLytter().join() }

        verify(exactly = 1) { healthService.addUnhealthyVote() }
        assertThat(exeption).isNull()
    }
}