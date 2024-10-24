package no.nav.arbeidsplassen.stihibi.bigquery.app.test

import no.nav.arbeidsplassen.stihibi.bigquery.app.env
import no.nav.arbeidsplassen.stihibi.startApp

abstract class TestRunningApplication {

    companion object {
        const val lokalBaseUrl = "http://localhost:8080"

        @JvmStatic
        val appCtx = TestApplicationContext(env)
        val javalin = appCtx.startApp()
    }

}