package se.zensum.ktorPrometheusFeature

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.response.respondTextWriter
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Summary
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.exporter.common.TextFormat

private const val DEFAULT_PATH = "/metrics"
private const val METRIC_NAME_PARAM = "name[]"

fun once(f: () -> Unit): () -> Unit {
    var triggered = false
    return {
        if (!triggered) {
            triggered = true
            f()
        }
    }
}

private fun setupServerInternal() {
    if (System.getenv("DISABLE_PROMETHEUS_SERVER") == "yes") {
        return
    }
    HTTPServer(9090)
}

private val setupServer = once(::setupServerInternal)

class PrometheusFeature(configuration: Configuration) {

    private val summary = configuration.summaryOrDefault()
    private val counter = configuration.counterOrDefault()
    private val exposeMetrics = configuration.hasMetricsEndpoint()
    init {
        setupServer.invoke()
    }


    class Configuration {
        internal fun summaryOrDefault() = summary ?: Summary.build()
                .name("http_request_duration_seconds")
                .help("Total response time")
                .register()
        internal fun counterOrDefault() = counter ?: Counter.build()
                .name("http_request")
                .labelNames("method", "response_code")
                .help("total number of requests")
                .register()

        var summary: Summary? = null
        var counter: Counter? = null
        internal var runServer: Boolean = true

        private var metricsEndpoint = true
        internal fun hasMetricsEndpoint() = metricsEndpoint
        fun disableMetricsEndpoint() { metricsEndpoint = false }
        fun disableServer() { runServer = false }
    }

    private fun countReq(method: String, statusCode: String?) {
        counter.labels(method, statusCode ?: "200").inc()
    }

    private suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {

        if (exposeMetrics && context.context.request.path() == DEFAULT_PATH) {
            val metricNames = context.call.parameters
                    .getAll(METRIC_NAME_PARAM)
                    .orEmpty()
                    .toSet()
            val mfs = CollectorRegistry.defaultRegistry
                    .filteredMetricFamilySamples(metricNames)
            context.call.respondTextWriter {
                TextFormat.write004(this, mfs)
            }
            context.finish()
            return
        }
        try {
            summary.startTimer().use {
                context.proceed()
            }
        } finally {
            countReq(
                    context.context.request.httpMethod.value,
                    context.context.response.status()?.value?.toString()
            )
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, PrometheusFeature> {
        override val key = AttributeKey<PrometheusFeature>("PrometheusFeature")
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): PrometheusFeature {
            val result = PrometheusFeature(Configuration().apply(configure))
            pipeline.intercept(ApplicationCallPipeline.Call) {
                result.intercept(this)
            }
            return result
        }
    }
}