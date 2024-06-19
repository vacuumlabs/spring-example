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
const val XRAY_PROPAGATOR_KEY = "xray"

// The currently supported propagators. This may grow in the future.
private val propagators = mapOf(
    W3C_BAGGAGE_PROPAGATOR_KEY to W3CBaggagePropagator.getInstance(),
    W3C_PROPAGATOR_KEY to W3CTraceContextPropagator.getInstance(),
    B3_PROPAGATOR_KEY to B3Propagator.injectingMultiHeaders(),
    XRAY_PROPAGATOR_KEY to AwsXrayPropagator.getInstance(),
)

/**
 * A [TextMapPropagator] for use by different services. Rather than enforcing a specific injection format such as
 * W3CTraceContext, it supports different options, including injecting the same format as was extracted to allow compatibility
 * with various applications. Also AWS XRay propagator is supported, which is useful for propagating trace context coming
 * from AWS services (Lambdas, AppSync, etc).
 */
class OtelCompositePropagator(
    /**
     * Configurable propagation strategy. It can also be configured using `OTEL_TRACING_PROPAGATION_STRATEGY` env var.
     * They will be used in the order they are listed.
     *
     * Possible values (this is also the default):
     * `"w3cBaggage,extracted,w3c,b3,xray"`
     *
     * Note: If you want to use the baggage propagator, it should be the first in the list.
     * TODO would be nice to allow configuring in app yaml?
     */
    private val propagationStrategy: List<String> =
        System.getenv("OTEL_TRACING_PROPAGATION_STRATEGY")
            ?.split(",")?.filter { it in propagators.keys || it == "extracted" }
            ?: listOf(
                W3C_BAGGAGE_PROPAGATOR_KEY,
                EXTRACTED_PROPAGATOR_KEY,
                W3C_PROPAGATOR_KEY,
                B3_PROPAGATOR_KEY,
                XRAY_PROPAGATOR_KEY,
            )
): TextMapPropagator {

    private val logger = LoggerFactory.getLogger(javaClass)!!

    private val useExtracted = propagationStrategy.contains(EXTRACTED_PROPAGATOR_KEY)
    private val extractedPropagatorCtxKey: ContextKey<TextMapPropagator> = ContextKey.named(EXTRACTED_PROPAGATOR_KEY)

    private val fields = propagators.filter { it.key in propagationStrategy }.values.flatMap { it.fields() }.toSet()

    override fun fields(): Collection<String> = fields

    init {
        logger.info("Using propagators: $propagationStrategy")
    }

    override fun <C:Any?> inject(context: Context, carrier: C?, setter: TextMapSetter<C>) {
        propagationStrategy.forEach { propagatorName ->
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
        logger.info("USED!!!!!")

        var currentCtx = context
        propagationStrategy.forEach { propagatorName ->
            if (propagatorName != EXTRACTED_PROPAGATOR_KEY) {
                val propagator = propagators[propagatorName] ?: return@forEach
                currentCtx = propagator.extract(currentCtx, carrier, getter)
                if (Span.fromContextOrNull(currentCtx) != null) {
                    logger.info("Extracted span using $propagatorName propagator")
                    logger.info("Extracted span ${Span.fromContextOrNull(currentCtx)?.spanContext?.traceId}")
                    if (useExtracted) currentCtx = currentCtx.with(extractedPropagatorCtxKey, propagator)
                    return currentCtx
                }
            }
        }

        logger.info("No trace id")
        return currentCtx
    }

}
