package no.nav.arbeidsplassen.stihibi

import io.micronaut.configuration.kafka.ConsumerRegistry
import javax.inject.Singleton

@Singleton
class KafkaStateRegistry {

    private val stateRegistry = hashMapOf<String, KafkaState>()

    fun setConsumerToError(consumer:String) {
        stateRegistry[consumer] = KafkaState.ERROR
    }

    fun setConsumerToPaused(consumer: String) {
        stateRegistry[consumer] = KafkaState.PAUSED
    }

    fun hasError(): Boolean {
       return (stateRegistry.isNotEmpty() && stateRegistry.values.contains(KafkaState.ERROR))
    }

}

enum class KafkaState {
    RUNNING, PAUSED, ERROR
}


