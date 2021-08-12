package no.nav.arbeidsplassen.stihibi

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@MicronautTest
class BigQueryServiceIT(private val bigQueryService: BigQueryService, private val objectMapper: ObjectMapper) {

    @Test
    fun pushToBigQuery() {
        val ad = objectMapper.readValue(BigQueryServiceIT::class.java.getResourceAsStream("/adto/fullAdDTO.json"), AdTransport::class.java)
        val ads = listOf<AdTransport>(ad)
        val response = bigQueryService.sendBatch(ads = ads, offsets = listOf(1), partitions = listOf(2), topic = "mytopic")
        Assertions.assertFalse(response.hasError)
        println(response)
    }

}
