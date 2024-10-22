package no.nav.arbeidsplassen.stihibi

import io.micronaut.runtime.Micronaut

object ApplicationMicronaut {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
                .packages("no.nav.arbeidsplassen.stihibi")
                .mainClass(ApplicationMicronaut.javaClass)
                .start()
    }
}
