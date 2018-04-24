package se.zensum.ktorPrometheusFeature

import io.ktor.content.OutgoingContent
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.prometheus.client.Collector
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import kotlinx.coroutines.experimental.io.ByteWriteChannel
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
) : OutgoingContent.WriteChannelContent() {
    override val status = HttpStatusCode.OK

    override suspend fun writeTo(channel: ByteWriteChannel) {
        val metrics = registry
                .filteredMetricFamilySamples(metricNames)
        channel.writeFully(metricsToStr(metrics))
    }
}