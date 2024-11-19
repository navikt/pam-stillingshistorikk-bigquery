package no.nav.arbeidsplassen.stillingshistorikk.app.test

import no.nav.arbeidsplassen.stillingshistorikk.app.env
import no.nav.arbeidsplassen.stillingshistorikk.startApp

abstract class TestRunningApplication {

    companion object {
        const val lokalBaseUrl = "http://localhost:8080"

        @JvmStatic
        val appCtx = TestApplicationContext(env)
        val javalin = appCtx.startApp()
    }

}