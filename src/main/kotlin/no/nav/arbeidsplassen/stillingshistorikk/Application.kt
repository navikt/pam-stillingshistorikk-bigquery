package no.nav.arbeidsplassen.stillingshistorikk

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.json.JavalinJackson
import io.javalin.micrometer.MicrometerPlugin
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRoute
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRouteSource
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
        accessManager = accessManager,
        setupRoutes = { config -> setupAllRoutes(config) }
    )

    startLyttere()

    return javalin
}

private fun ApplicationContext.setupAllRoutes(config: JavalinConfig) {
    naisController.setupRoutes(config)
    adAvvisningController.setupRoutes(config)
    adHistoryContoller.setupRoutes(config)
    administrationTimeController.setupRoutes(config)
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
    accessManager: JavalinAccessManager,
    setupRoutes: (JavalinConfig) -> Unit
): Javalin {
    val requestLogger = LoggerFactory.getLogger("access")
    val log = LoggerFactory.getLogger("no.nav.arbeidsplassen.stillingshistorikk")
    val micrometerPlugin = MicrometerPlugin { micrometerConfig ->
        micrometerConfig.registry = meterRegistry
    }

    return Javalin.create { config ->
        config.router.ignoreTrailingSlashes = true
        config.router.treatMultipleSlashesAsSingleSlash = true
        config.requestLogger.http { ctx, ms ->
            if (!(ctx.path().endsWith("/internal/isReady") ||
                        ctx.path().endsWith("/internal/isAlive") ||
                        ctx.path().endsWith("/internal/prometheus"))
            )
                logRequest(ctx, ms, requestLogger)
        }
        config.http.defaultContentType = "application/json"
        config.jsonMapper(jsonMapper)
        config.registerPlugin(micrometerPlugin)

        config.routes.beforeMatched { ctx ->
            ctx.endpoints().matchedHttpEndpoint()?.let { endepunkt ->
                HttpServerRoute.update(
                    io.opentelemetry.context.Context.current(),
                    HttpServerRouteSource.NESTED_CONTROLLER,
                    endepunkt.path
                )
            }
            if (ctx.routeRoles().isNotEmpty()) {
                accessManager.manage(ctx, ctx.routeRoles())
            }
        }
        config.routes.before { ctx ->
            val callId = ctx.header("Nav-Call-Id") ?: ctx.header("Nav-CallId") ?: UUID.randomUUID().toString()
            ctx.attribute("TraceId", callId)
            MDC.put("TraceId", callId)
        }
        config.routes.after {
            MDC.remove("TraceId")
            MDC.remove("U")
            MDC.remove(KONSUMENT_ID_MDC_KEY)
        }
        config.routes.exception(NotFoundException::class.java) { e, ctx ->
            log.info("NotFoundException: ${e.message}", e)
            ctx.status(404).result(e.message ?: "")
        }
        config.routes.exception(ForbiddenException::class.java) { e, ctx ->
            log.info("ForbiddenException: ${e.message}", e)
            ctx.status(403).result(e.message ?: "")
        }
        config.routes.exception(UnauthorizedException::class.java) { e, ctx ->
            log.info("UnauthorizedException: ${e.message}", e)
            ctx.status(401).result(e.message ?: "")
        }
        config.routes.exception(IllegalArgumentException::class.java) { e, ctx ->
            log.info("IllegalArgumentException: ${e.message}", e)
            ctx.status(400).result(e.message ?: "")
        }
        config.routes.exception(Exception::class.java) { e, ctx ->
            log.info("Exception: ${e.message}", e)
            ctx.status(500).result(e.message ?: "")
        }

        setupRoutes(config)
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