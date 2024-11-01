package no.nav.arbeidsplassen.stihibi

import io.javalin.Javalin
import io.javalin.http.Context
import no.nav.arbeidsplassen.stihibi.kafka.KafkaListener
import no.nav.arbeidsplassen.stihibi.sikkerhet.Rolle

class StatusController(
    private val kafkaLyttere: List<KafkaListener<*>>
) {
    fun setupRoutes(javalin: Javalin) {
        javalin.get("/internal/kafkaState/pause", { pause(it) }, Rolle.UNPROTECTED)
        javalin.get("/internal/kafkaState/resume", { gjenoppta(it) }, Rolle.UNPROTECTED)
    }

    private fun pause(ctx: Context) {
        kafkaLyttere.forEach { it.pauseLytter() }
        ctx.contentType("text/plain").result("Pauser alle lyttere")
    }
    private fun gjenoppta(ctx: Context) {
        kafkaLyttere.forEach { it.gjenopptaLytter() }
        ctx.contentType("text/plain").result("Gjenopptar alle lyttere")
    }
}