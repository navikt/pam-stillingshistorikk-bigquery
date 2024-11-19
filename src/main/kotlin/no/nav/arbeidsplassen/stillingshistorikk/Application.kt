package no.nav.arbeidsplassen.stillingshistorikk

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.json.JavalinJackson
import io.javalin.micrometer.MicrometerPlugin
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.arbeidsplassen.stillingshistorikk.config.hentKonsumentId
import no.nav.arbeidsplassen.stillingshistorikk.sikkerhet.ForbiddenException
import no.nav.arbeidsplassen.stillingshistorikk.sikkerhet.JavalinAccessManager
import no.nav.arbeidsplassen.stillingshistorikk.sikkerhet.NotFoundException
import no.nav.arbeidsplassen.stillingshistorikk.sikkerhet.UnauthorizedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*

fun main() {
    val log = LoggerFactory.getLogger("no.nav.arbeidsplassen.stillingshistorikk")

    try {
        val env = System.getenv()
        val appContext = ApplicationContext(env)
        appContext.startApp()
    } catch (e: Exception) {
        log.error("Uventet feil ved oppstart av applikasjon: ${e.message}", e)
    }
}

const val KONSUMENT_ID_MDC_KEY = "konsument_id"

fun ApplicationContext.startApp(): Javalin {
    val accessManager = JavalinAccessManager(tokenConfig.tokenValidationHandler())

    val javalin = startJavalin(
        port = 8080,
        jsonMapper = JavalinJackson(objectMapper),
        meterRegistry = prometheusRegistry,
        accessManager = accessManager
    )

    setupAllRoutes(javalin)
    startLyttere()

    return javalin
}

private fun ApplicationContext.setupAllRoutes(javalin: Javalin) {
    naisController.setupRoutes(javalin)
    statusController.setupRoutes(javalin)
    adAvvisningController.setupRoutes(javalin)
    adHistoryContoller.setupRoutes(javalin)
    administrationTimeController.setupRoutes(javalin)
}

private fun ApplicationContext.startLyttere() {
    if (env["ADLISTENER_ENABLED"] == "true") {
        kafkaLyttere.forEach { lytter -> lytter.startLytter() }
    }
}

fun startJavalin(
    port: Int = 8080,
    jsonMapper: JavalinJackson,
    meterRegistry: PrometheusMeterRegistry,
    accessManager: JavalinAccessManager
): Javalin {
    val requestLogger = LoggerFactory.getLogger("access")
    val log = LoggerFactory.getLogger("no.nav.arbeidsplassen.stillingshistorikk")
    val micrometerPlugin = MicrometerPlugin { micrometerConfig ->
        micrometerConfig.registry = meterRegistry
    }

    return Javalin.create {
        it.router.ignoreTrailingSlashes = true
        it.router.treatMultipleSlashesAsSingleSlash = true
        it.requestLogger.http { ctx, ms ->
            if (!(ctx.path().endsWith("/internal/isReady") ||
                        ctx.path().endsWith("/internal/isAlive") ||
                        ctx.path().endsWith("/internal/prometheus"))
            )
                logRequest(ctx, ms, requestLogger)
        }
        it.http.defaultContentType = "application/json"
        it.jsonMapper(jsonMapper)
        it.registerPlugin(micrometerPlugin)

    }.beforeMatched { ctx ->
        if (ctx.routeRoles().isEmpty()) {
            return@beforeMatched
        }
        accessManager.manage(ctx, ctx.routeRoles())

    }.before { ctx ->
        val callId = ctx.header("Nav-Call-Id") ?: ctx.header("Nav-CallId") ?: UUID.randomUUID().toString()
        ctx.attribute("TraceId", callId)
        MDC.put("TraceId", callId)
    }.after {
        MDC.remove("TraceId")
        MDC.remove("U")
        MDC.remove(KONSUMENT_ID_MDC_KEY)
    }.exception(NotFoundException::class.java) { e, ctx ->
        log.info("NotFoundException: ${e.message}", e)
        ctx.status(404).result(e.message ?: "")
    }.exception(ForbiddenException::class.java) { e, ctx ->
        log.info("ForbiddenException: ${e.message}", e)
        ctx.status(403).result(e.message ?: "")
    }.exception(UnauthorizedException::class.java) { e, ctx ->
        log.info("UnauthorizedException: ${e.message}", e)
        ctx.status(401).result(e.message ?: "")
    }.exception(IllegalArgumentException::class.java) { e, ctx ->
        log.info("IllegalArgumentException: ${e.message}", e)
        ctx.status(400).result(e.message ?: "")
    }.exception(Exception::class.java) { e, ctx ->
        log.info("Exception: ${e.message}", e)
        ctx.status(500).result(e.message ?: "")
    }.start(port)
}

fun logRequest(ctx: Context, ms: Float, log: Logger) {
    log.info(
        "${ctx.method()} ${ctx.url()} ${ctx.statusCode()}",
        kv("konsument_id", ctx.attribute<String>(KONSUMENT_ID_MDC_KEY)),
        kv("method", ctx.method()),
        kv("requested_uri", ctx.path()),
        kv("requested_url", ctx.url()),
        kv("protocol", ctx.protocol()),
        kv("status_code", ctx.statusCode()),
        kv("TraceId", "${ctx.attribute<String>("TraceId")}"),
        kv("U", "${ctx.attribute<String>("U")}"),
        kv(KONSUMENT_ID_MDC_KEY, "${ctx.hentKonsumentId()}"),
        kv("elapsed_ms", "$ms")
    )
}