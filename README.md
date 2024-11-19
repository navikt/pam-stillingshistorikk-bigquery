# stillingshistorikk

* Konsumerer stillinger fra internt kafka-topic
* Lagrer det i BigQuery
* Tilbyr et enkelt REST-API for å forespørre dataen

## Lokal kjøring

1. Bygg appen med gralde: `./gradlew build`.
1. Appen krever en lokal auth-server, det kan startes med `./start-docker-compose.sh`.
1. Kjør `LocalApplication.kt` for å starte applikasjonen.
