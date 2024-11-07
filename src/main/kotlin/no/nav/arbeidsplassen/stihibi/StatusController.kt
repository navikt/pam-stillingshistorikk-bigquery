package no.nav.arbeidsplassen.stihibi

import io.javalin.Javalin
import io.javalin.http.Context
import no.nav.arbeidsplassen.stihibi.kafka.KafkaListener
import no.nav.arbeidsplassen.stihibi.sikkerhet.Rolle

class StatusController(
    private val kafkaLyttere: List<KafkaListener<*>>
) {
    fun setupRoutes(javalin: Javalin) {
        javalin.get("/internal/kafkaState", { kafkaStatus(it) }, Rolle.UNPROTECTED)
        javalin.put("/internal/kafkaState/pause", { pause(it) }, Rolle.UNPROTECTED)
        javalin.put("/internal/kafkaState/resume", { gjenoppta(it) }, Rolle.UNPROTECTED)
    }

    private fun kafkaStatus(ctx: Context) {
        ctx.contentType("text/plain").result(
            kafkaLyttere.joinToString("\n") { it.harFeilet().toString() }
        )
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