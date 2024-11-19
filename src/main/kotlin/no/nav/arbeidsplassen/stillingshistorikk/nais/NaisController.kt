package no.nav.arbeidsplassen.stillingshistorikk.nais

import io.javalin.Javalin
import io.javalin.http.HttpStatus
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.arbeidsplassen.stillingshistorikk.sikkerhet.Rolle

class NaisController(
    private val healthService: HealthService, private val prometheusMeterRegistry: PrometheusMeterRegistry
) {
    fun setupRoutes(javalin: Javalin) {
        javalin.get("/internal/isReady", { it.status(200) }, Rolle.UNPROTECTED)
        javalin.get(
            "/internal/isAlive",
            { if (healthService.isHealthy()) it.status(HttpStatus.OK) else it.status(HttpStatus.SERVICE_UNAVAILABLE) },
            Rolle.UNPROTECTED
        )
        javalin.get(
            "/prometheus",
            { it.contentType(TextFormat.CONTENT_TYPE_004).result(prometheusMeterRegistry.scrape()) },
            Rolle.UNPROTECTED
        )
    }
}

