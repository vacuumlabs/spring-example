package com.vacuumlabs.example.telemetry

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationPredicate
import io.micrometer.tracing.otel.bridge.OtelPropagator
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import org.springframework.boot.actuate.autoconfigure.tracing.SdkTracerProviderBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.observation.ServerRequestObservationContext

/*
 * Spring Micrometer tracing configuration, with OpenTelemetry.
 * Includes predicates to skip tracing on non-business endpoints such as actuator and swagger.
 */
@Configuration
class TracingConfig {

    @Bean
    fun otelPropagator(otelConfig: OtelConfig, otelTracer: Tracer): OtelPropagator {
        return OtelPropagator(
            ContextPropagators.create(OtelCompositePropagator(
                propagationStrategy = otelConfig.tracing.propagation.strategy,
                extractStrategy = otelConfig.tracing.propagation.extractStrategy,
                injectStrategy = otelConfig.tracing.propagation.injectStrategy,
            )),
            otelTracer
        )
    }

    @Bean
    fun customizer(otelConfig: OtelConfig): SdkTracerProviderBuilderCustomizer {
        return SdkTracerProviderBuilderCustomizer { builder ->
            builder.addSpanProcessor(
                BatchSpanProcessor.builder(
                    OtlpGrpcSpanExporter.builder()
                        .setEndpoint(otelConfig.exporter.endpoint)
                        .build()
                ).build()
            )
        }
    }

    @Bean
    fun noSpringSecurity(): ObservationPredicate {
        return ObservationPredicate { name: String, context: Observation.Context ->
            !name.startsWith("spring.security.")
        }
    }

    @Bean
    fun noActuator(): ObservationPredicate {
        return predicateForPath("/actuator")
    }

    @Bean
    fun noSwagger(): ObservationPredicate {
        return predicateForPath("/swagger")
    }

    @Bean
    fun noWebjars(): ObservationPredicate {
        return predicateForPath("/v3/api-docs")
    }

    private fun predicateForPath(path: String): ObservationPredicate {
        return ObservationPredicate { name: String, context: Observation.Context ->
            if (context is ServerRequestObservationContext)
                !context.carrier.requestURI.contains(path)
            else true
        }
    }
}
