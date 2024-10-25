package no.nav.arbeidsplassen.stihibi.bigquery.app.test

import com.google.cloud.NoCredentials
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.DatasetInfo
import no.nav.arbeidsplassen.stihibi.ApplicationContext
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.BigQueryEmulatorContainer
import org.testcontainers.containers.wait.strategy.Wait

/*
 * Application context som kan brukes i tester, denne inkluderer en egen mock-oauth2-server
 */
class TestApplicationContext(
    private val localEnv: MutableMap<String, String>,
    val localBigQuery: BigQueryEmulatorContainer = BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.6.5")
        .waitingFor(Wait.defaultWaitStrategy())
        .apply { start() }
        .also { container ->
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

    private val log: Logger = LoggerFactory.getLogger("LocalApplicationContext")

    val mockOauth2Server = MockOAuth2Server().also { server ->
        server.start()

        localEnv["AZURE_APP_WELL_KNOWN_URL"] = server.wellKnownUrl("azuread").toString()
        localEnv["TOKEN_X_WELL_KNOWN_URL"] = server.wellKnownUrl("tokenx").toString()

        log.info("Mock Oauth2 server azuread well known url: ${server.wellKnownUrl("azuread")}")
        log.info("Mock Oauth2 server tokenx well known url: ${server.wellKnownUrl("tokenx")}")
    }

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
                bigQuery.create(DatasetInfo.of(adSchemaTableDefinition.dataSet))
            }
    }
}