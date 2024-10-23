package no.nav.arbeidsplassen.stihibi.bigquery.app.local

import com.fasterxml.jackson.databind.JsonNode
import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.*
import com.google.cloud.bigquery.JobStatistics.LoadStatistics
import com.google.common.io.Files
import no.nav.arbeidsplassen.stihibi.AdTransport
import no.nav.arbeidsplassen.stihibi.ApplicationContext
import org.testcontainers.containers.BigQueryEmulatorContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.nio.channels.Channels
import java.util.*


/*
 * Application context som kan brukes å state appen lokalt, her kan det f.eks. settes opp en lokal postgres og kafka.
 */
class LocalApplicationContext(
    private val localEnv: MutableMap<String, String>,
    val localBigQuery: BigQueryEmulatorContainer = BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.6.5").waitingFor(
        Wait.defaultWaitStrategy()
    ).apply { start() }.also { container ->
        localEnv["BIGQUERY_PROJECT_ID"] = container.projectId
        localEnv["BIGQUERY_ENDPOINT"] = container.emulatorHttpEndpoint
        println("Started BigQuery Emulator on ${container.emulatorHttpEndpoint} with pid: ${container.projectId}")
    }
//    val localKafka : Any = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.2"))
//        .withKraft()
//        .waitingFor(Wait.defaultWaitStrategy())
//        .apply { start() }
//        .also { localConfig ->
//            localEnv["KAFKA_BROKERS"] = localConfig.bootstrapServers
//        }
) : ApplicationContext(localEnv) {


    override val bigQuery: BigQuery by lazy {
        BigQueryOptions.newBuilder()
            .setProjectId(localEnv["BIGQUERY_PROJECT_ID"])
            .setHost(localEnv["BIGQUERY_ENDPOINT"])
            .setLocation(localEnv["BIGQUERY_ENDPOINT"])
            .setCredentials(NoCredentials.getInstance())
            .build()
            .service.also { bigQuery ->
                // Oppretter dataset og tabell for den lokale instansen, se: https://stackoverflow.com/a/45526059
                val datasetId: DatasetId = DatasetId.of(adSchemaTableDefinition.dataSet)
                val datasetInfo: DatasetInfo = DatasetInfo.of(datasetId)
                bigQuery.create(datasetInfo)

                val tableId: TableId = TableId.of(adSchemaTableDefinition.dataSet, adSchemaTableDefinition.tableNameV1)
                val tableDefinition: TableDefinition = StandardTableDefinition.of(adSchemaTableDefinition.schemaV1)
                val tableInfo: TableInfo = TableInfo.of(tableId, tableDefinition)
                bigQuery.create(tableInfo)
            }
    }
}