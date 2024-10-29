package no.nav.arbeidsplassen.stihibi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.arbeidsplassen.stihibi.api.v1.AdAvvisningContoller
import no.nav.arbeidsplassen.stihibi.api.v1.AdHistoryContoller
import no.nav.arbeidsplassen.stihibi.api.v1.AdministrationTimeController
import no.nav.arbeidsplassen.stihibi.config.TokenConfig
import no.nav.arbeidsplassen.stihibi.kafka.KafkaConfig
import no.nav.arbeidsplassen.stihibi.kafka.KafkaRapidJsonListener
import no.nav.arbeidsplassen.stihibi.kafka.KafkaRapidListener
import no.nav.arbeidsplassen.stihibi.nais.HealthService
import no.nav.arbeidsplassen.stihibi.nais.NaisController
import java.net.http.HttpClient
import java.util.*

open class ApplicationContext(envInn: Map<String, String>) {
    val env: Map<String, String> by lazy { envInn }

    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))

    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT).also { registry ->
        ClassLoaderMetrics().bindTo(registry)
        JvmMemoryMetrics().bindTo(registry)
        JvmGcMetrics().bindTo(registry)
        JvmThreadMetrics().bindTo(registry)
        UptimeMetrics().bindTo(registry)
        ProcessorMetrics().bindTo(registry)
    }

    val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .version(HttpClient.Version.HTTP_1_1)
        .build()

    val tokenConfig = TokenConfig(env)

    val healthService = HealthService()

    val kafkaConfig = KafkaConfig(env)

    val kafkaLyttere by lazy { kafkaLyttere() }

    val naisController = NaisController(healthService, prometheusRegistry)

    open val bigQuery: BigQuery by lazy { BigQueryOptions.getDefaultInstance().service }
    val adSchemaTableDefinition = AdSchemaTableDefinition(objectMapper)
    private val bigQueryService by lazy {
        BigQueryService(adSchemaTableDefinition, bigQuery, objectMapper)
    }
    val adAvvisningController by lazy { AdAvvisningContoller(bigQueryService, objectMapper) }
    val adHistoryContoller by lazy { AdHistoryContoller(bigQueryService, objectMapper) }
    val administrationTimeController by lazy { AdministrationTimeController(bigQueryService) }

    private fun kafkaLyttere(): List<KafkaRapidListener<*>> {
        val lyttere = mutableListOf<KafkaRapidListener<*>>()

        val adTopicConsumer by lazy { AdTopicListener(bigQueryService, env.getValue("STILLING-HISTORIKK_TOPIC"), objectMapper) }
        val adTopicConsumerConfig = kafkaConfig.kafkaJsonConsumer(env.getValue("STILLING-HISTORIKK_TOPIC"), env.getValue("STIHIBI_GROUP_ID"))
        val adTopicListener by lazy { KafkaRapidJsonListener(adTopicConsumerConfig, adTopicConsumer, healthService) }
        lyttere.add(adTopicListener)

        return lyttere
    }
}