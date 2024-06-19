package com.vacuumlabs.example.telemetry

//import io.micrometer.tracing.otel.bridge.*
//import io.opentelemetry.api.OpenTelemetry
//import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
//import io.opentelemetry.context.propagation.ContextPropagators
//import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
//import io.opentelemetry.sdk.OpenTelemetrySdk
//import io.opentelemetry.sdk.trace.SdkTracerProvider
//import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
//import io.opentelemetry.sdk.trace.samplers.Sampler
//import org.springframework.boot.actuate.autoconfigure.tracing.SdkTracerProviderBuilderCustomizer
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationPredicate
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.observation.ServerRequestObservationContext


/*
 * Spring Micrometer tracing configuration.
 * Includes predicates to skip tracing on non-business endpoints such as actuator and swagger
 */
@Configuration
class TracingConfig {

//    @Bean
//    fun openTelemetry(): OpenTelemetry {
//        val spanExporter = OtlpGrpcSpanExporter.builder()
//            .setEndpoint("http://localhost:4317")
//            .build()
//
//        val sdkTracerProvider = SdkTracerProvider.builder()
//            .setSampler(Sampler.alwaysOn()) // tail sampling in collector is recommended
//            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
//            .build()
//
//        val otel = OpenTelemetrySdk.builder()
//            .setTracerProvider(sdkTracerProvider)
//            .setPropagators(ContextPropagators.create(OtelCompositePropagator(/*todo*/)))
//            .buildAndRegisterGlobal()
//
//        val otelTracer: Tracer = otel.tracerProvider.get("org.springframework.boot") // "io.micrometer.micrometer-tracing")
//        val otelCurrentTraceContext = OtelCurrentTraceContext()
//        OtelTracer(
//            otelTracer,
//            otelCurrentTraceContext,
//            { event: Any? ->
//                Slf4JEventListener().onEvent(event)
//                Slf4JBaggageEventListener(emptyList()).onEvent(event)
//            },
//            OtelBaggageManager(otelCurrentTraceContext, emptyList(), emptyList())
//        )
//
//        return otel
//    }

//    @Bean
//    fun otelPropagator(otelTracer: Tracer): OtelPropagator {
//        return OtelPropagator(
//            ContextPropagators.create(W3CTraceContextPropagator.getInstance()),
////            ContextPropagators.create(OtelCompositePropagator(/*todo*/)),
//            otelTracer // openTelemetry.tracerProvider.get("org.springframework.boot") // "io.micrometer.micrometer-tracing")
//        )
//    }
//
//    @Bean
//    fun customizer(): SdkTracerProviderBuilderCustomizer {
//        return SdkTracerProviderBuilderCustomizer { builder ->
//            builder.addSpanProcessor(
//                BatchSpanProcessor.builder(
//                    OtlpGrpcSpanExporter.builder()
//                        .setEndpoint("http://localhost:4317")
//                        .build()
//                ).build()
//            )
//        }
//    }

    @Bean
    fun openTelemetry(): OpenTelemetry {
        val tracerProvider = SdkTracerProvider.builder().build()

        val openTelemetrySdk = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal()

        return openTelemetrySdk
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
