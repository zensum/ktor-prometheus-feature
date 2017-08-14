package se.zensum.ktorPrometheusFeature

import io.prometheus.client.Counter
import io.prometheus.client.Summary
import org.jetbrains.ktor.application.ApplicationCall
import org.jetbrains.ktor.application.ApplicationCallPipeline
import org.jetbrains.ktor.application.ApplicationFeature
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.pipeline.PipelineContext
import org.jetbrains.ktor.request.httpMethod
import org.jetbrains.ktor.util.AttributeKey

class PrometheusFeature(configuration: Configuration) {

    val summary = configuration.summary
    val counter = configuration.counter

    class Configuration {
        var summary: Summary? = null
        var counter: Counter? = null
    }

    suspend fun intercept(context: PipelineContext<ApplicationCall>){
        var timer: Summary.Timer? = null
        try {
            if(summary != null) { timer = summary.startTimer() }
            context.proceed()
            context.finish()
        } catch (e: Exception) {
            context.call.response.status(HttpStatusCode.fromValue(500))
            throw e
        } finally {
            if(timer != null){  summary!!.observe(timer.observeDuration()) }
            if(counter != null) {
                val method = context.call.request.httpMethod.value
                val responseCode = context.call.response.status()!!.value.toString()
                counter.labels(method, responseCode).inc()
            }
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