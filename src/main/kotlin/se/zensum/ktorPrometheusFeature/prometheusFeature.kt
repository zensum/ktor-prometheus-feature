package se.zensum.ktorPrometheusFeature

import io.prometheus.client.Counter
import io.prometheus.client.Summary
import org.jetbrains.ktor.application.ApplicationCallPipeline
import org.jetbrains.ktor.application.ApplicationFeature
import org.jetbrains.ktor.pipeline.PipelineContext
import org.jetbrains.ktor.request.httpMethod
import org.jetbrains.ktor.request.path
import org.jetbrains.ktor.util.AttributeKey

private const val DEFAULT_PATH = "/metrics"
private const val METRIC_NAME_PARAM = "name[]"

class PrometheusFeature(configuration: Configuration) {

    private val summary = configuration.summaryOrDefault()
    private val counter = configuration.counterOrDefault()
    private val exposeMetrics = configuration.hasMetricsEndpoint()

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

        private var metricsEndpoint = true
        internal fun hasMetricsEndpoint() = metricsEndpoint
        fun disableMetricsEndpoint() { metricsEndpoint = false }
    }

    private fun countReq(method: String, statusCode: String?) {
        counter.labels(method, statusCode ?: "200").inc()
    }

    private suspend fun intercept(context: PipelineContext<Unit>) {
        if (exposeMetrics && context.call.request.path() == DEFAULT_PATH) {
            val metricNames = context.call.parameters
                    .getAll(METRIC_NAME_PARAM)
                    .orEmpty()
                    .toSet()
            call.respond(PrometheusResponder(metricNames = metricNames))
            context.finish()
            return
        }
        try {
            summary.startTimer().use {
                context.proceed()
            }
        } finally {
            countReq(
                    context.call.request.httpMethod.value,
                    context.call.response.status()?.value?.toString()
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