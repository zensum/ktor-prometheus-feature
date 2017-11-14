package se.zensum.ktorPrometheusFeature

import io.prometheus.client.Collector
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import org.jetbrains.ktor.cio.WriteChannel
import org.jetbrains.ktor.content.FinalContent
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.util.ValuesMap
import java.io.CharArrayWriter
import java.nio.CharBuffer
import java.util.*

private const val INITIAL_METRICS_BUFFER = 1024

private fun metricsToStr(
        metrics: Enumeration<Collector.MetricFamilySamples>
) =
        CharArrayWriter(INITIAL_METRICS_BUFFER)
        .also { TextFormat.write004(it, metrics) }
        .toCharArray()
        .let { CharBuffer.wrap(it) }
        .let {  Charsets.UTF_8.encode(it) }


internal class PrometheusResponder(
        val registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
        val metricNames: Set<String> = emptySet()
) : FinalContent.WriteChannelContent() {
    override val status = HttpStatusCode.OK
    override val headers: ValuesMap
        get() = ValuesMap.build {
            append("Content-Type", TextFormat.CONTENT_TYPE_004)
        }

    suspend override fun writeTo(channel: WriteChannel) {
        val metrics = registry
                .filteredMetricFamilySamples(metricNames)
        channel.write(metricsToStr(metrics))
    }
}