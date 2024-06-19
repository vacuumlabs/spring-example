package com.vacuumlabs.example

import io.opentelemetry.api.trace.Span
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

@RestController
class HelloController(
    private val restTemplate: RestTemplate,
) {
    val logger = LoggerFactory.getLogger(javaClass)!!

    @GetMapping
    fun hello(@RequestHeader("traceparent") traceparent: String): String {
        logger.info("Hello endpoint called with traceparent: $traceparent",)
        val currentSpan = Span.current();
        logger.info("Current trace ID: {}", currentSpan.spanContext.traceId)

        this.restTemplate.getForObject<String>("http://localhost:8080/bye")

        return "Hello, World!"
    }

    @GetMapping("/bye")
    fun bye(): String {
        logger.info("Bye endpoint called")
        val currentSpan = Span.current();
        logger.info("Current trace ID: {}", currentSpan.spanContext.traceId)

        return "Bye, World!"
    }
}
