package se.zensum.ktorPrometheusFeature

import io.prometheus.client.Counter
import io.prometheus.client.Summary
import org.jetbrains.ktor.application.ApplicationCallPipeline
import org.jetbrains.ktor.application.ApplicationFeature
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.pipeline.PipelineContext
import org.jetbrains.ktor.request.httpMethod
import org.jetbrains.ktor.util.AttributeKey

class PrometheusFeature(configuration: Configuration) {

    val summary = configuration.summaryOrDefault()
    val counter = configuration.counterOrDefault()

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
    }

    suspend fun intercept(context: PipelineContext<Unit>) {
        var timer = summary.startTimer()
        try {
            context.proceed()
            context.finish()
        } catch (e: Exception) {
            context.call.response.status(HttpStatusCode.fromValue(500))
            throw e
        } finally {
            summary.observe(timer.observeDuration())
            val method = context.call.request.httpMethod.value
            val responseCode = if (context.call.response.status() != null) context.call.response.status()!!.value.toString() else "404"
            counter.labels(method, responseCode).inc()
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