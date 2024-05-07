package com.vacuumlabs.example

import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.with
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class ContainerConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        return PostgreSQLContainer(DockerImageName.parse("postgres:14-alpine"))
    }

    @Bean
    fun kafkaContainer(properties: DynamicPropertyRegistry): KafkaContainer {
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.1.2"))
        properties.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers)
        return kafka
    }
}

fun main(args: Array<String>) {
    fromApplication<ExampleApplication>().with(ContainerConfiguration::class).run(*args)
}
