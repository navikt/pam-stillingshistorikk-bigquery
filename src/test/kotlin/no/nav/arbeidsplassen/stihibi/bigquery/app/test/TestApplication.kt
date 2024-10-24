package no.nav.arbeidsplassen.stihibi.bigquery.app.test

import no.nav.arbeidsplassen.stihibi.bigquery.app.env
import no.nav.arbeidsplassen.stihibi.startApp

fun main() {
    val localAppCtx = TestApplicationContext(env)
    localAppCtx.startApp()
}