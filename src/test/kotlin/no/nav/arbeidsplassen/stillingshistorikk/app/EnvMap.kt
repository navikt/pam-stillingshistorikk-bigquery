package no.nav.arbeidsplassen.stillingshistorikk.app

val env = mutableMapOf(
    "TOKEN_X_CLIENT_ID" to "tokenxClientId",
    "TOKEN_X_PRIVATE_JWK" to "privateJwk",
    "TOKEN_X_ISSUER" to "tokenx",
    "TOKEN_X_TOKEN_ENDPOINT" to "MOCK",
    "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "http://localhost/noe_mock_server_greier",
    "AZURE_APP_CLIENT_ID" to "azureClientId",
    "AZURE_APP_CLIENT_SECRET" to "hemmelig",
    "ADLISTENER_ENABLED" to "true",
    "ADLISTENER_TOPIC" to "teampam.stilling-historikk",
    "ADLISTENER_GROUP_ID" to "stillingshistorikk",
)