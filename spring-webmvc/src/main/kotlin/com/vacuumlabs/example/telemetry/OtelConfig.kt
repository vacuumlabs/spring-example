package com.vacuumlabs.example.telemetry

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "otel")
class OtelConfig(
    val exporter: Exporter,
    val tracing: Tracing,
) {
    class Exporter(
        val endpoint: String = "http://localhost:4317"
    )

    class Tracing(
        val propagation: Propagation
    ) {
        class Propagation(
            val strategy: String? = null,
            val extractStrategy: String? = null,
            val injectStrategy: String? = null,
        )
    }
}
