# Spring example project

This repo contains several Spring Boot example projects to demonstrate various features available in
the Spring ecosystem in a purposefully opinionated way to help showcase best practices. Each example
microservice is a separate subproject in the `spring-apps` directory, which is the root project of
the gradle build.

These projects can also be used as components to test drive other things, such as CICD pipelines /
Github workflows, monitoring, etc.

Links to the individual projects / readmes:

- [Spring WebMVC](spring-webmvc/README.md)
- [Spring JPA](spring-jpa)
- [Spring cloud-stream-kafka](spring-cloud-stream-kafka)

## More docs

- [Observability for Spring](spring-webmvc%2Fsrc%2Fmain%2Fkotlin%2Fcom%2Fvacuumlabs%2Fexample%2Ftelemetry%2FREADME.md)
- Blueprints
  - [Gradle Build](docs%2Fblueprints%2FGradleBuild.md)
  - [Gradle Build for Kotlin JVM projects](docs%2Fblueprints%2FGradleBuildKotlinJvm.md)
