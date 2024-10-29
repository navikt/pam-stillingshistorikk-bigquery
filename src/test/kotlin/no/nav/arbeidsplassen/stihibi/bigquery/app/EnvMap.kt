package no.nav.arbeidsplassen.stihibi.bigquery.app

val env = mutableMapOf(
    "TOKEN_X_CLIENT_ID" to "tokenxClientId",
    "TOKEN_X_PRIVATE_JWK" to "privateJwk",
    "TOKEN_X_ISSUER" to "tokenx",
    "TOKEN_X_TOKEN_ENDPOINT" to "MOCK",
    "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "http://localhost/noe_mock_server_greier",
    "AZURE_APP_CLIENT_ID" to "azureClientId",
    "AZURE_APP_CLIENT_SECRET" to "hemmelig",
    "KAFKA_SCHEMA_REGISTRY" to "http://localhost:8081",
    "KAFKA_SCHEMA_REGISTRY_USER" to "user",
    "KAFKA_SCHEMA_REGISTRY_PASSWORD" to "pwd",
    "START_KAFKA_KONSUMENTER" to "true",
    "STILLING-HISTORIKK_TOPIC" to "teampam.stilling-historikk",
    "STIHIBI_GROUP_ID" to "pam-stihibi",
)