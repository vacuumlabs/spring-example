package com.vacuumlabs.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringCloudStreamKafkaApplication

fun main(args: Array<String>) {
    runApplication<SpringCloudStreamKafkaApplication>(*args)
}
