package no.nav.arbeidsplassen.stillingshistorikk.sikkerhet

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.time.LocalDate

@JsonDeserialize(using = FodselsnummerDeserializer::class)
data class Fodselsnummer(val fodselsnummer: String) {
    companion object {
        fun of(fnr: String?) = fnr?.let { Fodselsnummer(it) }
    }

    fun erUnderFemten(): Boolean {
        return toLocalDate().isAfter(LocalDate.now().minusYears(15))
    }

    fun toLocalDate(): LocalDate {
        val fnr = fodselsnummer

        /** Tar for seg Fødselsnummer og D-nummer (Som legger til 4 på første siffer)
         * og syntetiske fnr som har måneder som starter på 80
         * dollypersoner har fnr som starter på 40
         * npid (bost) starter på 60. Merk at vi kunne brukt modulo 20 for å ta alle disse tilfellene
         * Dette er egentlig ikke noen god ide. En persons alder bør ikke finnes ut fra
         * identen, men ut fra pdl
         *
         * NB: Her tar vi ikke hensyn til riktig gamle folk og individnumrene i personnummeret.
         * Vi legger bare til 2000 til fødselsår og trekker fra 100 hvis det indikerer at man er født i fremtiden.
         * Dette gir feil for de som er over 100, men det er ikke så mange av de i løsningen vår. For å finne riktig alder
         * bør vi bruke PDL, og hvis det ikke går, bruke de tre første sifrene i personnummeret.
         * Da får man følgende regel:
         * Etter år 2000 har det vært gjort et antall endringer angående fastsettelse av individsifre,
         * og pr. oktober 2013 benyttes denne inndelingen:
        000 – 499 omfatter personer født i perioden 1900 – 1999.
        500 – 749 omfatter personer født i perioden 1854 – 1899.
        500 – 999 omfatter personer født i perioden 2000 – 2039.
        900 – 999 omfatter personer født i perioden 1940 – 1999.
         */
        var dd = fnr.slice(0..1).toInt()
        var mm = fnr.slice(2..3).toInt()
        var yyyy = fnr.slice(4..5).toInt() + 2000

        if (dd > 40) dd -= 40
        if (mm > 40 && mm < 53) mm -= 40
        if (mm > 60 && mm < 63) mm -= 60
        if (mm > 80) mm -= 80
        if (yyyy > LocalDate.now().year) yyyy -= 100

        return LocalDate.of(yyyy, mm, dd)
    }
}

class FodselsnummerDeserializer : JsonDeserializer<Fodselsnummer>() {
    override fun deserialize(parser: JsonParser, deserializationContext: DeserializationContext): Fodselsnummer {
        val fnrAsString = parser.text
        return Fodselsnummer(fnrAsString)
    }
}
