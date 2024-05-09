package com.vacuumlabs.example

import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.with
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class ContainerConfiguration {
    @Bean
    fun kafkaContainer(properties: DynamicPropertyRegistry): KafkaContainer {
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.1.2"))
        properties.add("spring.cloud.stream.kafka.binder.brokers", kafka::getBootstrapServers)
        return kafka
    }
}

fun main(args: Array<String>) {
    fromApplication<SpringCloudStreamKafkaApplication>().with(ContainerConfiguration::class).run(*args)
}
