package no.nav.arbeidsplassen.stillingshistorikk.sikkerhet

import io.javalin.http.Context
import no.nav.arbeidsplassen.stillingshistorikk.config.getAccessToken
import no.nav.security.token.support.client.core.context.JwtBearerTokenResolver
import no.nav.security.token.support.core.context.TokenValidationContext

/**
 * Token resolverne blir brukt for å slippe å sende rundt en javalin Context til tjenestelaget, men heller
 * pakke det inn i resolverklasser
 */
class ContextBearerTokenResolver(private val ctx: Context) : JwtBearerTokenResolver {
    override fun token(): String? =
        ctx.getAccessToken()
}

class ValidationContextBearerTokenResolver(private val ctx: TokenValidationContext) : JwtBearerTokenResolver {
    override fun token(): String? =
        ctx.firstValidToken?.encodedToken
}
