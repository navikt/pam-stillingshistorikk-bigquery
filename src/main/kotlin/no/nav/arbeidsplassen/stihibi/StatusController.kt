package no.nav.arbeidsplassen.stihibi

import io.micronaut.configuration.kafka.ConsumerRegistry
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import org.slf4j.LoggerFactory

@Controller("/internal")
class StatusController(private val kafkaStateRegistry: KafkaStateRegistry, private val consumerRegistry: ConsumerRegistry) {

    companion object {
        private val LOG = LoggerFactory.getLogger(StatusController::class.java)
    }

    @Get("/isReady")
    fun isReady(): String {
        return "OK"
    }

    @Get("/isAlive")
    fun isAlive(): HttpResponse<String> {
        if (kafkaStateRegistry.hasError()) {
            LOG.error("A Kafka consumer is set to Error, setting all consumers to pause")
            consumerRegistry.consumerIds
                .forEach {
                    if (!consumerRegistry.isPaused(it)) {
                        LOG.error("Pausing consumer $it")
                        consumerRegistry.pause(it)
                    }
                }
            // not necessary to restart the app.
            //return HttpResponse.serverError("Kafka consumer is not running")
        }
        return HttpResponse.ok("OK")
    }

    @Get("/kafkaState")
    fun setKafkaState(): Boolean {
        return kafkaStateRegistry.hasError()
    }

}
