package no.nav.arbeidsplassen.stihibi

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import java.time.format.DateTimeFormatter
import javax.inject.Singleton

@Singleton
class AdSchemaTableDefinition(private val objectMapper: ObjectMapper) {

    val bqDatetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")

    val schemaV1: Schema = Schema.of(
        Field.of("uuid", StandardSQLTypeName.STRING),
        Field.of("source", StandardSQLTypeName.STRING),
        Field.of("created", StandardSQLTypeName.DATETIME),
        Field.of("updated", StandardSQLTypeName.DATETIME),
        Field.of("published", StandardSQLTypeName.DATETIME),
        Field.of("expires", StandardSQLTypeName.DATETIME),
        Field.of("publishedByAdmin", StandardSQLTypeName.DATETIME),
        Field.of("status", StandardSQLTypeName.STRING),
        Field.of("adminIdent", StandardSQLTypeName.STRING),
        Field.of("adminStatus", StandardSQLTypeName.STRING),
        Field.of("medium", StandardSQLTypeName.STRING),
        Field.of("title", StandardSQLTypeName.STRING),
        Field.of("updatedBy", StandardSQLTypeName.STRING),
        Field.of("businessName", StandardSQLTypeName.STRING),
        Field.of("orgNr", StandardSQLTypeName.STRING),
        Field.of("kafkaOffset", StandardSQLTypeName.STRING),
        Field.of("kafkaPartition", StandardSQLTypeName.STRING),
        Field.of("kafkaTopic", StandardSQLTypeName.STRING),
        Field.of("json", StandardSQLTypeName.STRING))

    val tableNameV1 = "stilling_historikk_v1"
    val dataSet = "stilling_historikk_bq"

    fun toRowDefinition(ad: AdTransport, kafkaOffset:Long, kafkaPartition: Int, kafkaTopic: String): Map<String,Any?> {
        return HashMap<String,Any?>().apply {
            put("uuid", ad.uuid)
            put("source", ad.source)
            put("created", ad.created.format(bqDatetimeFormatter))
            put("updated", ad.updated.format(bqDatetimeFormatter))
            if (ad.published!=null) put("published", ad.published.format(bqDatetimeFormatter))
            put("expires",ad.expires.format(bqDatetimeFormatter))
            if (ad.publishedByAdmin!=null) put("publishedByAdmin", ad.publishedByAdmin.format(bqDatetimeFormatter))
            put("status", ad.status)
            put("adminIdent", ad.administration.navIdent)
            put("adminStatus", ad.administration.status)
            put("medium", ad.medium)
            put("title", ad.title)
            put("updatedBy", ad.updatedBy)
            put("businessName", ad.businessName)
            if (ad.employer!=null) put("orgNr", ad.employer.orgnr)
            put("kafkaOffset", kafkaOffset)
            put("kafkaPartition", kafkaPartition)
            put("kafkaTopic", kafkaTopic)
            put("json", objectMapper.writeValueAsString(ad))
        }
    }
}

