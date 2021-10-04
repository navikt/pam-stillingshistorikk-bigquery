package no.nav.arbeidsplassen.stihibi

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.*
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.LocalDate

@Singleton
class BigQueryService(
    private val adSchemaTableDefinition: AdSchemaTableDefinition,
    private val objectMapper: ObjectMapper
) {


    private val bq = BigQueryOptions.getDefaultInstance().service
    private val tableId: TableId = TableId.of(adSchemaTableDefinition.dataSet, adSchemaTableDefinition.tableNameV1)
    private val tableFNAME = "${bq.options.projectId}.${tableId.dataset}.${tableId.table}"

    companion object {
        private val LOG = LoggerFactory.getLogger(BigQueryService::class.java)
    }

    init {
        val table = createTable()
        if (table != null) {
            LOG.info("We are using bigquery table {}", tableFNAME)
        } else {
            LOG.error("Could not find or create table in bigquery")
        }
    }

    fun sendBatch(
        ads: List<AdTransport>,
        offsets: List<Long>,
        partitions: List<Int>,
        topics: List<String>
    ): BigQueryResponse {
        val request = InsertAllRequest.newBuilder(tableId)
        for (i in ads.indices) {
            request.addRow(adSchemaTableDefinition.toRowDefinition(ads[i], offsets[i], partitions[i], topics[i]))
        }
        val response = bq.insertAll(request.build())
        if (response.hasErrors()) {
            LOG.error(response.insertErrors.values.toString())
            return BigQueryResponse(hasError = true, rowsError = response.insertErrors.size)
        }
        return BigQueryResponse(hasError = false, rowsError = 0)
    }

    private fun createTable(): Table? {
        return bq.getTable(tableId) ?: createTableWithPartition()
    }

    private fun createTableWithPartition(): Table {
        val partitioning = TimePartitioning.newBuilder(TimePartitioning.Type.YEAR)
            .setField("created") //  name of column to use for partitioning
            .build()
        val tableDefinition = StandardTableDefinition.newBuilder()
            .setSchema(adSchemaTableDefinition.schemaV1)
            .setTimePartitioning(partitioning)
            .build()
        val tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
        return bq.create(tableInfo)
    }

    fun queryAdHistory(uuid: String, year: Int): List<AdTransport> {
        val query =
            """SELECT json FROM `${tableFNAME}` WHERE uuid = @uuid AND EXTRACT(YEAR FROM created) = @year ORDER BY updated asc LIMIT 1000"""
        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("uuid", QueryParameterValue.string(uuid))
            .addNamedParameter("year", QueryParameterValue.int64(year))
            .build()
        val results: TableResult = bq.query(queryConfig)
        return results.iterateAll().map {
            objectMapper.readValue(it["json"].value.toString(), AdTransport::class.java)
        }.toList()
    }

    fun queryAdministrationTime(from: LocalDate, to: LocalDate): String {
        val query = """
            SELECT * FROM (
            SELECT LAST_DAY(DATE '2021-09-01') as Periode, Kilde, APPROX_QUANTILES(admintime,100)[SAFE_OFFSET(50)] AS Median, AVG(admintime) AS Gjennomsnitt FROM (
            SELECT * FROM (
            SELECT t0.behandlingstid AS admintime, t0.source AS Kilde FROM (
            WITH pending as (SELECT uuid, adminStatus, source,  MIN(updated) as starttime from `${tableFNAME}` where adminStatus='PENDING' and updatedBy='nss-admin' and created >= @from and created < @to group by uuid, adminStatus, source),
            done as (SELECT uuid, adminStatus, source, MIN(Updated) as endtime from `${tableFNAME}` where adminStatus='DONE' and updatedBy='nss-admin' and created >= @from and created < @to group by uuid, adminStatus, source) 
            SELECT pending.uuid, pending.starttime, done.endtime, pending.source, timestamp_diff(endtime, starttime, SECOND) as behandlingstid FROM pending, done where pending.uuid = done.uuid and pending.source!='ASS' and pending.source!='DIR'
            ) t0
            ) WHERE (admintime < 28800)
            ) GROUP BY Kilde
            ) LIMIT 20000000
        """.trimIndent()
        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("from", QueryParameterValue.string(from.toString()))
            .addNamedParameter("to", QueryParameterValue.string(to.toString()))
            .build()
        val results: TableResult = bq.query(queryConfig)
        val strBuilder = StringBuilder()
        strBuilder.append("Periode;Kilde;Median;Gjennomsnitt")
        results.iterateAll().map {
            strBuilder.append("\n${it["Periode"].value};${it["Kilde"].value};${it["Median"].value};${it["Gjennomsnitt"].value}")
        }
        return strBuilder.toString()
    }
}

data class BigQueryResponse(val hasError: Boolean, val rowsError: Int)
