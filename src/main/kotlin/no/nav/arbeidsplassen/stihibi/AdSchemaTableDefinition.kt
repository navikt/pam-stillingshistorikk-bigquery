package no.nav.arbeidsplassen.stihibi

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName

class AdSchemaTableDefinition(private val objectMapper: ObjectMapper) {

    val tableNameV1 = "stilling_historikk_v1"
    val dataSet = "stilling_historikk_bq"

    val schemaV1: Schema = Schema.of(
        Field.of("uuid", StandardSQLTypeName.STRING),
        Field.of("id", StandardSQLTypeName.INT64),
        Field.of("reference", StandardSQLTypeName.STRING),
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
        Field.of("json", StandardSQLTypeName.STRING),
        Field.of("md5", StandardSQLTypeName.STRING))

    fun toRowDefinition(ad: AdTransport, kafkaOffset:Long, kafkaPartition: Int, kafkaTopic: String): Map<String,Any?> {
        return HashMap<String,Any?>().apply {
            put("uuid", ad.uuid)
            put("id", ad.id)
            put("reference", ad.reference)
            put("source", ad.source)
            put("created", ad.created.toBqDateTime())
            put("updated", ad.updated.toBqDateTime())
            if (ad.published!=null) put("published", ad.published.toBqDateTime())
            put("expires",ad.expires.toBqDateTime())
            if (ad.publishedByAdmin!=null) put("publishedByAdmin", ad.publishedByAdmin.toBqDateTime())
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
            val jsonString = objectMapper.writeValueAsString(ad)
            put("json", jsonString)
            put("md5", jsonString.toMD5Hex())
        }
    }
}

