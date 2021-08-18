package no.nav.arbeidsplassen.stihibi

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

// This test requires access to bigquery, useful to test bigquery.
@MicronautTest
class BigQueryServiceIT(private val bigQueryService: BigQueryService, private val objectMapper: ObjectMapper) {

    @Test
    fun pushToBigQuery() {
        val ad = objectMapper.readValue(BigQueryServiceIT::class.java.getResourceAsStream("/adto/fullAdDTO.json"), AdTransport::class.java)
        val ads = listOf<AdTransport>(ad)
        val response = bigQueryService.sendBatch(ads = ads, offsets = listOf(1), partitions = listOf(2), topics = listOf("mytopic"))
        Assertions.assertFalse(response.hasError)
        println(response)
    }

    @Test
    fun queryBigQuery() {
        val ads = bigQueryService.queryAdHistory("e282c8c3-2c2b-4d9e-bfab-d1804df4c9de", 2021)
    }
}
