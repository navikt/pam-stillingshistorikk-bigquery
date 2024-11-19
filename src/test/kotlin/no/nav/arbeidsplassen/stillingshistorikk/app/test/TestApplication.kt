package no.nav.arbeidsplassen.stillingshistorikk.app.test

import no.nav.arbeidsplassen.stillingshistorikk.app.env
import no.nav.arbeidsplassen.stillingshistorikk.startApp

fun main() {
    val localAppCtx = TestApplicationContext(env)
    localAppCtx.startApp()
}