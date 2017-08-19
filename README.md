# ktor-promethues - Prometheus request logging for all requests
[![license](https://img.shields.io/github/license/zensum/ktor-prometheus-feature.svg)]() [![](https://jitpack.io/v/zensum/ktor-prometheus-feature.svg)](https://jitpack.io/#zensum/ktor-promethues-feature)

Ktor-prometheus adds support for request logging with prometheus in [ktor](https://ktor.io).

```kotlin
import se.zensum.ktorPrometheusFeature

embeddedServer(Netty, 8080) {
    install(PrometheusFeature)
}.start(wait = true)
```

Just install the function and two metrics are added `http_request`,
containing two labels one for the HTTP method and one for the status
code, and `http_duration_seconds` tracking the processing time of each
request.


If you want to use other names for the metrics feel it is simply a
matter passing them along in the configuration

```kotlin import
se.zensum.ktorPrometheusFeature

// We write the method and status code, in that order into the labels and those must therefore exist.
val totalRequests = Counter.build().name("http_total_requests").labelNames("method", "status_code").help("Total number of requests").register()
val totalTimer = Summary.build().name("http_total_timer").help("Total response time").register()

embeddedServer(Netty, 8080) {
    install(PrometheusFeature) {
        // Configuration block. If these variables are not set, they default to null and wont be used
        counter = totalRequests // Sets a Prometheus counter to be increased for each request.
        summary = totalTimer    // Sets a Promethues timer to count the total response time of the requests
    }
}.start(wait = true)
```

## Installation
First add jitpack.io to your dependencies

``` gradle
maven { url 'https://jitpack.io' }
```

And then our dependency

``` gradle
dependencies {
            compile 'com.github.zensum:ktor-prometheus-feature:-SNAPSHOT'
}
```
