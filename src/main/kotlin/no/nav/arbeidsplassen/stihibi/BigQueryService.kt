package no.nav.arbeidsplassen.stihibi

import com.google.cloud.bigquery.*
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
class BigQueryService(private val adSchemaTableDefinition: AdSchemaTableDefinition) {


    private val bq = BigQueryOptions.getDefaultInstance().service
    private val tableId: TableId = TableId.of(adSchemaTableDefinition.dataSet,adSchemaTableDefinition.tableNameV1)
    private val tableInfo: TableInfo = TableInfo.newBuilder(tableId, StandardTableDefinition.of(adSchemaTableDefinition.schemaV1)).build()

    companion object {
        private val LOG = LoggerFactory.getLogger(BigQueryService::class.java)
    }

    init {
        val table = createTable()
        if (table!=null) {
            LOG.info("We are using bigquery table {}", table.tableId.table)
        }
        else {
            LOG.error("Could not find or create table in bigquery")
        }
    }

    fun sendBatch(ads: List<AdTransport>, offsets: List<Long>, partitions: List<Int>,topics: List<String>):BigQueryResponse {
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

    private fun createTable():Table? {
        return bq.getTable(tableId) ?: bq.create(tableInfo)
    }
}

data class BigQueryResponse(val hasError: Boolean, val rowsError: Int)
