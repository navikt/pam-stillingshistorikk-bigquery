package no.nav.arbeidsplassen.stillingshistorikk

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
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.arbeidsplassen.stillingshistorikk.api.AdAvvisningContoller
import no.nav.arbeidsplassen.stillingshistorikk.api.AdHistoryContoller
import no.nav.arbeidsplassen.stillingshistorikk.api.AdministrationTimeController
import no.nav.arbeidsplassen.stillingshistorikk.config.TokenConfig
import no.nav.arbeidsplassen.stillingshistorikk.kafka.KafkaConfig
import no.nav.arbeidsplassen.stillingshistorikk.kafka.KafkaListener
import no.nav.arbeidsplassen.stillingshistorikk.nais.HealthService
import no.nav.arbeidsplassen.stillingshistorikk.nais.NaisController
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
        LogbackMetrics().bindTo(registry)
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
    val adSchemaTableDefinition = AdSchemaTableDefinition(env.getValue("ADLISTENER_GROUP_ID"), env.getValue("GOOGLE_BIGQUERY_DATASET"), objectMapper)
    val bigQueryService by lazy { BigQueryService(adSchemaTableDefinition, bigQuery, objectMapper) }
    val adAvvisningController by lazy { AdAvvisningContoller(bigQueryService, objectMapper) }
    val adHistoryContoller by lazy { AdHistoryContoller(bigQueryService, objectMapper) }
    val administrationTimeController by lazy { AdministrationTimeController(bigQueryService) }

    private fun kafkaLyttere(): List<KafkaListener<*>> {
        val lyttere = mutableListOf<KafkaListener<*>>()

        val topic = env.getValue("ADLISTENER_TOPIC")
        val adTopicConsumerConfig = kafkaConfig.kafkaJsonConsumer(topic, env.getValue("ADLISTENER_GROUP_ID"))
        val adTopicListener by lazy { AdTopicListener(adTopicConsumerConfig, bigQueryService, topic, objectMapper, healthService) }
        lyttere.add(adTopicListener)

        return lyttere
    }
}