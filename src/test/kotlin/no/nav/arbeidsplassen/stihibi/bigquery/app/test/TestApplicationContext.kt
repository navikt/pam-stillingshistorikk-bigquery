package no.nav.arbeidsplassen.stihibi.bigquery.app.test

import no.nav.arbeidsplassen.stihibi.ApplicationContext
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/*
 * Application context som kan brukes i tester, denne inkluderer en egen mock-oauth2-server
 */
class TestApplicationContext(
    private val localEnv: MutableMap<String, String>,
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
}