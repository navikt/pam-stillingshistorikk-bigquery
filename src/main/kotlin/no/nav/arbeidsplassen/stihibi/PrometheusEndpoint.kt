package no.nav.arbeidsplassen.stihibi

import io.micrometer.prometheus.PrometheusMeterRegistry
import io.micronaut.management.endpoint.annotation.Endpoint
import io.micronaut.management.endpoint.annotation.Read

@Endpoint(id ="myprometheus", defaultSensitive = false)
class PrometheusEndpoint(private val prometheusMeterRegistry: PrometheusMeterRegistry) {

    @Read(produces = ["text/plain; version=0.0.4"])
    fun scrape(): String {
        return prometheusMeterRegistry.scrape()
    }
}
