package no.nav.arbeidsplassen.stihibi.bigquery.app.local

import com.fasterxml.jackson.databind.JsonNode
import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.*
import no.nav.arbeidsplassen.stihibi.*
import org.testcontainers.containers.BigQueryEmulatorContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.ConfluentKafkaContainer

/*
 * Application context som kan brukes å state appen lokalt, her kan det f.eks. settes opp en lokal postgres og kafka.
 */
class LocalApplicationContext(
    private val localEnv: MutableMap<String, String>,
    val localBigQuery: BigQueryEmulatorContainer = BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.6.5")
        .waitingFor(Wait.defaultWaitStrategy())
        .apply { start() }
        .also { container ->
            localEnv["BIGQUERY_PROJECT_ID"] = container.projectId
            localEnv["BIGQUERY_ENDPOINT"] = container.emulatorHttpEndpoint
            println("Started BigQuery Emulator on ${container.emulatorHttpEndpoint} with pid: ${container.projectId}")
        },
    val localKafka: Any = ConfluentKafkaContainer("confluentinc/cp-kafka:7.7.0")
        .waitingFor(Wait.defaultWaitStrategy())
        .apply { start() }
        .also { localConfig ->
            localEnv["KAFKA_BROKERS"] = localConfig.bootstrapServers
        }
) : ApplicationContext(localEnv) {
    override val bigQuery: BigQuery by lazy {
        BigQueryOptions.newBuilder()
            .setProjectId(localEnv["BIGQUERY_PROJECT_ID"])
            .setHost(localEnv["BIGQUERY_ENDPOINT"])
            .setLocation(localEnv["BIGQUERY_ENDPOINT"])
            .setCredentials(NoCredentials.getInstance())
            .build()
            .service
            .also { bigQuery ->
                // Oppretter dataset for den lokale instansen
                val adSchemaTD = adSchemaTableDefinition
                bigQuery.create(DatasetInfo.of(adSchemaTD.dataSet))
                bigQuery.create(
                    TableInfo.of(
                        TableId.of(adSchemaTD.dataSet, adSchemaTD.tableNameV1),
                        StandardTableDefinition.of(adSchemaTD.schemaV1)
                    )
                )
                val testData = javaClass.getResourceAsStream("/stillinger-behandlet-av-nss.json")
                val stillingerJSON = objectMapper.readValue(testData, JsonNode::class.java)
                val request = InsertAllRequest.newBuilder(TableId.of(adSchemaTD.dataSet, adSchemaTD.tableNameV1))
                stillingerJSON.map { rad ->
                    request.addRow(adSchemaTD.toRowDefinition((objectMapper.readValue(rad.get("json").asText(), AdTransport::class.java)), 0, 0, ""))
                }
                bigQuery.insertAll(request.build())
            }
    }
}