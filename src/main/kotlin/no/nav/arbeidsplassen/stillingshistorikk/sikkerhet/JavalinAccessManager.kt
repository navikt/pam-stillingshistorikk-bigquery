package no.nav.arbeidsplassen.stillingshistorikk.sikkerhet

import io.javalin.http.Context
import io.javalin.http.HttpResponseException
import io.javalin.security.RouteRole
import no.nav.arbeidsplassen.stillingshistorikk.KONSUMENT_ID_MDC_KEY
import no.nav.arbeidsplassen.stillingshistorikk.config.*
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.http.HttpRequest
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler
import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 */
class JavalinAccessManager(private val tokenValidationHandler: JwtTokenValidationHandler) {
    companion object {
        private val LOG = LoggerFactory.getLogger(JavalinAccessManager::class.java)
    }

    fun manage(ctx: Context, routeRoles: Set<RouteRole>) {
        require(routeRoles.size == 1) { "Støtter kun bruk av en rolle per endepunkt." }
        require(routeRoles.first() is Rolle) { "Ukonfigurert rolle" }
        val rolle = routeRoles.first() as Rolle

        if (rolle == Rolle.UNPROTECTED) {
            return
        } else {
            val tokenValidationContext = tokenValidationHandler.getValidatedTokens(ctx.httpRequest)
            if (tokenValidationContext.hasValidToken()) {
                ctx.setTokenValidationContext(tokenValidationContext)
                val claims = tokenValidationContext.firstValidToken?.jwtTokenClaims
                claims?.let {
                    ctx.setClaims(it)
                    val fnr = Fodselsnummer.of(it.getStringClaim("pid"))
                    fnr?.let { f -> ctx.setFødselsnummer(f) }
                    val navIdent = it.getStringClaim("NAVident")
                    navIdent?.let { n -> ctx.setNAVIdent(n) }
                    ctx.setAccessToken(hentAccessTokenFraHeader(ctx))

                    val clientId = it.getStringClaim("client_id")
                    val azpName = it.getStringClaim("azp_name")
                    val konsumentId = clientId ?: azpName ?: ""
                    ctx.attribute(KONSUMENT_ID_MDC_KEY, konsumentId)
                    MDC.put(KONSUMENT_ID_MDC_KEY, konsumentId)

                    val ident = fnr?.fodselsnummer ?: navIdent ?: ""
                    MDC.put("U", ident)
                    ctx.attribute("U", ident)
                }
            } else {
                LOG.warn("${rolle.name} er IKKE autorisert")
                throw HttpResponseException(401, "Unauthorized")
            }
        }
    }

    private fun hentAccessTokenFraHeader(context: Context): String {
        val accessTokenMedBearerPrefix = context.httpRequest.getHeader("Authorization")
            ?: throw IllegalStateException("Prøvde å hente ut access token men Authorization header finnes ikke")

        return accessTokenMedBearerPrefix.replace("Bearer ", "", ignoreCase = true)
    }

    private val Context.httpRequest: HttpRequest
        get() = object : HttpRequest {
            override fun getHeader(headerName: String): String? = headerMap()[headerName]
        }
}

enum class Rolle : RouteRole { PÅLOGGET, TEAMPAM, UNPROTECTED }

enum class TokenUtsteder {
    TOKEN_X,
    AZURE_AD,
    INGEN
}

/**
 * Henter fnr fra tokenx claims.
 */
@Throws(ForbiddenException::class)
fun TokenValidationContext.fnrFromClaims(): Fodselsnummer? {
    val fnrFromClaims: String = this.getClaims("tokenx")
        .let {
            it.getStringClaim("pid") ?: it.subject
        } ?: ""
        .trim { it <= ' ' }

    return if (fnrFromClaims.isNotEmpty()) {
        Fodselsnummer.of(fnrFromClaims)
    } else {
        null
    }
}

fun TokenValidationContext.emailFromClaims(): String? {
    return when {
        this.hasTokenFor("aad") -> {
            this.getClaims("aad").getStringClaim("preferred_username")
        }

        else -> null
    }
}


// Resulterer i 403 Forbidden
class ForbiddenException : RuntimeException {
    constructor(cause: Throwable) : super(cause)
    constructor(msg: String) : super(msg)
    constructor(msg: String, cause: Throwable) : super(msg, cause)
    constructor() : super()
}

// Resulterer i 401 Unauthorized
class UnauthorizedException : RuntimeException {
    constructor(cause: Throwable) : super(cause)
    constructor(msg: String) : super(msg)
    constructor(msg: String, cause: Throwable) : super(msg, cause)
    constructor() : super()
}

class NotFoundException : RuntimeException {
    constructor(cause: Throwable) : super(cause)
    constructor(msg: String) : super(msg)
    constructor(msg: String, cause: Throwable) : super(msg, cause)
    constructor() : super()
}
