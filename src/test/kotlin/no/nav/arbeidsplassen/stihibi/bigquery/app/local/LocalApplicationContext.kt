package no.nav.arbeidsplassen.stihibi.bigquery.app.local

import no.nav.arbeidsplassen.stihibi.ApplicationContext

/*
 * Application context som kan brukes å state appen lokalt, her kan det f.eks. settes opp en lokal postgres og kafka.
 */
class LocalApplicationContext(
    private val localEnv: MutableMap<String, String>,
//    val localPostgres : Any = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
//        .waitingFor(Wait.forListeningPort())
//        .apply { start() }
//        .also { localConfig ->
//            localEnv["DB_HOST"] = localConfig.host
//            localEnv["DB_PORT"] = localConfig.getMappedPort(5432).toString()
//        },
//    val localKafka : Any = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.2"))
//        .withKraft()
//        .waitingFor(Wait.defaultWaitStrategy())
//        .apply { start() }
//        .also { localConfig ->
//            localEnv["KAFKA_BROKERS"] = localConfig.bootstrapServers
//        }
) : ApplicationContext(localEnv)