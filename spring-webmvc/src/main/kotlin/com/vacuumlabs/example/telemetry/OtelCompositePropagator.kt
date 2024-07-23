package com.vacuumlabs.example.telemetry

import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.ContextKey
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapSetter
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator
import io.opentelemetry.extension.trace.propagation.B3Propagator
import io.opentelemetry.extension.trace.propagation.JaegerPropagator
import org.slf4j.LoggerFactory

/**
 * W3C Baggage propagator, used for user defined context propagation. Should be first in the list if used.
 */
const val W3C_BAGGAGE_PROPAGATOR_KEY = "w3cBaggage"
/**
 * Inject (propagate further) using same propagator as was used to extract the context.
  */
const val EXTRACTED_PROPAGATOR_KEY = "extracted"
const val W3C_PROPAGATOR_KEY = "w3c"
const val B3_PROPAGATOR_KEY = "b3"
const val B3_MULTI_PROPAGATOR_KEY = "b3multi"
const val JAEGER_PROPAGATOR = "jaeger"
const val XRAY_PROPAGATOR_KEY = "xray"

private val defaultExtractors = listOf(
    W3C_BAGGAGE_PROPAGATOR_KEY, EXTRACTED_PROPAGATOR_KEY, W3C_PROPAGATOR_KEY, XRAY_PROPAGATOR_KEY
)
private val defaultInjectors = listOf(
    W3C_BAGGAGE_PROPAGATOR_KEY, EXTRACTED_PROPAGATOR_KEY, W3C_PROPAGATOR_KEY
)

// The currently supported propagators. This may grow in the future.
private val commonPropagators = mapOf(
    W3C_BAGGAGE_PROPAGATOR_KEY to W3CBaggagePropagator.getInstance(),
    W3C_PROPAGATOR_KEY to W3CTraceContextPropagator.getInstance(),
    B3_PROPAGATOR_KEY to B3Propagator.injectingSingleHeader(),
    B3_MULTI_PROPAGATOR_KEY to B3Propagator.injectingMultiHeaders(),
    JAEGER_PROPAGATOR to JaegerPropagator.getInstance(),
    XRAY_PROPAGATOR_KEY to AwsXrayPropagator.getInstance(),
)

/**
 * A [TextMapPropagator] for use by different services. Rather than enforcing a specific injection format such as
 * W3CTraceContext, it supports different options, including injecting the same format as was extracted to allow compatibility
 * with various applications. Also, AWS XRay propagator is supported, which is useful for propagating trace context coming
 * from AWS services (Lambdas, AppSync, etc).
 *
 * @property customPropagators Custom propagators to be added to list of default that can be used. Refer to them using
 * their key.
 *
 * @property propagationStrategy Configurable propagation strategy. It can also be configured using
 * OTEL_TRACING_PROPAGATION_STRATEGY env var. Propagators will be used in the order they are listed. Use this if you want
 * same injectors and extractors.
 * Possible values (plus any custom propagators you provide): `"w3cBaggage,w3c,b3,b3multi,jaeger,xray,extracted"`.
 * Note: If you want to use the baggage propagator, it should be the first in the list.
 *
 * @property extractStrategy Configurable propagation strategy for extracting context from incoming requests. It can
 * also be configured using OTEL_TRACING_EXTRACT_PROPAGATION_STRATEGY env var. See [propagationStrategy] for more
 * details. Default is [propagationStrategy] or [defaultExtractors].
 *
 * @property injectStrategy Configurable propagation strategy for injecting context into outgoing requests. It can
 * also be configured using OTEL_TRACING_INJECT_PROPAGATION_STRATEGY env var. See [propagationStrategy] for more
 * details. Default is [propagationStrategy] or [defaultInjectors].
 *
 * @constructor Creates a new instance of [OtelCompositePropagator], customized using constructor parameters.
 */
class OtelCompositePropagator(
    private val customPropagators: Map<String, TextMapPropagator> = emptyMap(),
    private val propagationStrategy: String? = System.getenv("OTEL_TRACING_PROPAGATION_STRATEGY"),
    private val extractStrategy: String? =
        System.getenv("OTEL_TRACING_EXTRACT_PROPAGATION_STRATEGY") ?: propagationStrategy,
    private val injectStrategy: String? =
        System.getenv("OTEL_TRACING_INJECT_PROPAGATION_STRATEGY") ?: propagationStrategy,
): TextMapPropagator {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    private val propagators = commonPropagators + customPropagators

    private val extractors = selectPropagators(extractStrategy, defaultExtractors)
    private val injectors = selectPropagators(injectStrategy, defaultInjectors)

    private val useExtracted =
        extractors.contains(EXTRACTED_PROPAGATOR_KEY) && injectors.contains(EXTRACTED_PROPAGATOR_KEY)
    private val extractedPropagatorCtxKey: ContextKey<TextMapPropagator> = ContextKey.named(EXTRACTED_PROPAGATOR_KEY)

    init {
        logger.info("Using extracting propagator strategy: $extractors")
        logger.info("Using injecting propagator strategy: $injectors")
    }

    private val fields = propagators
        .filter { it.key in injectors || it.key in extractors }
        .values.flatMap { it.fields() }
        .toSet()
    override fun fields(): Collection<String> = fields

    override fun <C:Any?> inject(context: Context, carrier: C?, setter: TextMapSetter<C>) {
        injectors.forEach { propagatorName ->
            when(propagatorName) {
                W3C_BAGGAGE_PROPAGATOR_KEY ->
                    if (Baggage.fromContextOrNull(context) != null)
                        W3CBaggagePropagator.getInstance().inject(context, carrier, setter)
                EXTRACTED_PROPAGATOR_KEY ->
                    context.get(extractedPropagatorCtxKey)?.let {
                        it.inject(context, carrier, setter)
                        return@inject
                    }
                else ->
                    propagators[propagatorName]?.let {
                    it.inject(context, carrier, setter)
                    return@inject
                }
            }
        }
    }

    override fun <C> extract(context: Context, carrier: C?, getter: TextMapGetter<C>): Context {
        var currentCtx = context
        extractors.forEach { propagatorName ->
            if (propagatorName != EXTRACTED_PROPAGATOR_KEY) {
                val propagator = propagators[propagatorName] ?: return@forEach
                currentCtx = propagator.extract(currentCtx, carrier, getter)
                if (Span.fromContextOrNull(currentCtx)?.spanContext?.isValid == true) {
                    if (useExtracted) currentCtx = currentCtx.with(extractedPropagatorCtxKey, propagator)
                    return currentCtx
                }
            }
        }
        return currentCtx
    }

    private fun selectPropagators(strategy: String?, default: List<String>): List<String> =
        (strategy?.split(",") ?: default)
            .map {
                if (it == EXTRACTED_PROPAGATOR_KEY || propagators[it] != null) it
                else throw IllegalArgumentException("Unknown propagator key: $it")
            }
}
