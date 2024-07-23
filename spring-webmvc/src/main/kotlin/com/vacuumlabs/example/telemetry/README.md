# Observability

This module provides a way to collect telemetry data from the application. It is based on Micrometer
library, which is a common for Spring Boot applications, and OpenTelemetry which is collection of
APIs, SDKs and tools for observability.

## Logging

Logs are configured in the [logback-spring.xml](../../../../../resources/logback-spring.xml).
When running service locally we use `json-file-logging` profile which prints json logs into file in
[data/logs](../../../../../../../data/logs) folder, this can be read by `fluentbit` (... and so logs
eventually get to Grafana).

## Metrics

All Micrometer metrics are enabled in the [application.yml](../../../../../resources/application.yaml).

```yaml
management:
  metrics:
    enable:
      all: true
```

Prometheus compatible endpoint with all collected metrics is exposed on `/actuator/prometheus` path,
thanks to `io.micrometer:micrometer-registry-prometheus` dependency.

## Tracing

Some configuration is in [application.yml](../../../../../resources/application.yaml) file
(sampling, custom config mapped to [OtelConfig.kt](OtelConfig.kt)), but it is put together and
extended in [TracingConfig.kt](TracingConfig.kt).

Noticeable features

 - Ignore health check, swagger and actuator endpoints for telemetry purposes
 - Use GRPC Span exporter (with configured endpoint)
 - Use custom Tracing context propagator (with configurable extractors and injectors)

### Custom tracing context propagator

The custom [OtelCompositePropagator.kt](OtelCompositePropagator.kt) is used to propagate the tracing
context between services. It builds tracing context from data extracted from incoming headers, and
on outgoing requests it injects the tracing context to carriers so it gets into headers.

Our propagator is a composition of more propagators that can be freely specified and ordered, both
for extracting and injecting. It is often useful to have multiple extractors, because although many
services use w3c, if running in AWS environment, for example AWS AppSync will send traces in AWS
format, so we need to support both.

There is also special - `extracted` - propagator that is used to inject the tracing context using
same format as it was extracted.

## Further reading

- [Micrometer documentation](https://micrometer.io/docs)
- [Spring Boot Actuator documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [OpenTelemetry documentation](https://opentelemetry.io/docs/)
```
