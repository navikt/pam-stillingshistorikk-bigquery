package no.nav.arbeidsplassen.stillingshistorikk.config

import io.javalin.http.Context
import no.nav.arbeidsplassen.stillingshistorikk.KONSUMENT_ID_MDC_KEY
import no.nav.arbeidsplassen.stillingshistorikk.sikkerhet.AzureClientCredentialsKlient
import no.nav.arbeidsplassen.stillingshistorikk.sikkerhet.AzureOBOKlient
import no.nav.arbeidsplassen.stillingshistorikk.sikkerhet.Fodselsnummer
import no.nav.arbeidsplassen.stillingshistorikk.sikkerhet.TokendingsKlient
import no.nav.security.token.support.core.configuration.IssuerProperties
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler
import java.net.URI
import java.net.http.HttpClient

class TokenConfig(private val env: Map<String, String>) {

    fun issuers(): MultiIssuerConfiguration {
        val isserPropertiesMap = mutableMapOf<String, IssuerProperties>(
            "tokenx" to IssuerProperties(
                URI(env.lesEnvVarEllerKastFeil("TOKEN_X_WELL_KNOWN_URL")).toURL(),
                listOf(env.lesEnvVarEllerKastFeil("TOKEN_X_CLIENT_ID"))
            ),
            "azuread" to IssuerProperties(
                URI(env.lesEnvVarEllerKastFeil("AZURE_APP_WELL_KNOWN_URL")).toURL(),
                listOf(env.lesEnvVarEllerKastFeil("AZURE_APP_CLIENT_ID"))
            )
        )
        return MultiIssuerConfiguration(isserPropertiesMap)
    }

    fun tokenValidationHandler() = JwtTokenValidationHandler(issuers())

    fun azureOBOKlient(httpClient: HttpClient): AzureOBOKlient {
        return AzureOBOKlient(
            azureUrl = env.lesEnvVarEllerKastFeil("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            clientId = env.lesEnvVarEllerKastFeil("AZURE_APP_CLIENT_ID"),
            clientSecret = env.lesEnvVarEllerKastFeil("AZURE_APP_CLIENT_SECRET"),
            httpClient = httpClient
        )
    }

    fun azureClientCredentialsKlient(httpClient: HttpClient): AzureClientCredentialsKlient {
        return AzureClientCredentialsKlient(
            azureUrl = env.lesEnvVarEllerKastFeil("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            clientId = env.lesEnvVarEllerKastFeil("AZURE_APP_CLIENT_ID"),
            clientSecret = env.lesEnvVarEllerKastFeil("AZURE_APP_CLIENT_SECRET"),
            httpClient = httpClient
        )
    }

    fun tokenXClient(httpClient: HttpClient): TokendingsKlient {
        return TokendingsKlient(
            env.lesEnvVarEllerKastFeil("TOKEN_X_TOKEN_ENDPOINT"),
            env.lesEnvVarEllerKastFeil("TOKEN_X_PRIVATE_JWK"),
            env.lesEnvVarEllerKastFeil("TOKEN_X_CLIENT_ID"),
            env.lesEnvVarEllerKastFeil("TOKEN_X_ISSUER"),
            httpClient = httpClient
        )
    }
}

fun Map<String, String>.lesEnvVarEllerKastFeil(envVarNavn: String): String =
    this[envVarNavn] ?: throw IllegalArgumentException("$envVarNavn er ikke satt")

fun Context.setNAVIdent(ident: String) {
    attribute("NAVIdent", ident)
}

fun Context.hentNAVIdent(): String? =
    attribute("NAVIdent")

fun Context.hentFødselsnummer(): Fodselsnummer? =
    attribute("fnr")

fun Context.setFødselsnummer(fnr: Fodselsnummer) {
    attribute("fnr", fnr)
}

fun Context.hentKonsumentId(): String? =
    attribute(KONSUMENT_ID_MDC_KEY)

fun Context.setClaims(claims: JwtTokenClaims) {
    attribute("claims", claims)
}

fun Context.getClaims(): JwtTokenClaims? =
    attribute("claims")

fun Context.setAccessToken(token: String) {
    attribute("access_token", token)
}

fun Context.getAccessToken(): String? =
    attribute("access_token")

fun Context.setTokenValidationContext(context: TokenValidationContext) {
    attribute("tvcontext", context)
}

fun Context.getTokenValidationContext(): TokenValidationContext? =
    attribute("tvcontext")

fun Context.hentIssuer(): String? = getTokenValidationContext()?.issuers?.firstOrNull()

// Sikrer at body kun inneholder tegn som er lovlig JSON
fun Context.sanitizedBody(): String =
    this.body().replace(Regex("[^\\u0009\\u000a\\u000d\\u0020-\\uD7FF\\uE000-\\uFFFD]"), " ")
        .replace("\\u0000", "")
