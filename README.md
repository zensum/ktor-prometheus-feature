# ktor-prometheus-feature

# Example of installation and configuration


# ktor-promethues - Prometheus request logging for all requests
[![license](https://img.shields.io/github/license/zensum/ktor-prometheus-feature.svg)]() [![](https://jitpack.io/v/zensum/ktor-prometheus-feature.svg)](https://jitpack.io/#zensum/ktor-promethues-feature)

Ktor-prometheus adds support for request logging with prometheus
in [ktor](https://ktor.io). Set a promethues Counter for the total amount of requests, including method and response code and a Summary
to count the total excecution times of the requests.

When setting up the feature, a Prometheus Summary and Counter must be set. If these are not set, there is no point in
using this feature as it wont do anything.

```kotlin
import se.zensum.ktorPrometheusFeature

// In the implementaion of the Counter, it writes labels in the order of {"HTTP_METHOD", "HTTP_RESPONSE_CODE"} and expects these label names to exists.
val totalRequests = Counter.build().name("http_total_requests").labelNames("method", "response_code").help("Total number of requests").register()
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