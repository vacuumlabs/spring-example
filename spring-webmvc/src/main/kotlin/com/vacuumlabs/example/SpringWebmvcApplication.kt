package com.vacuumlabs.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class SpringWebmvcApplication

fun main(args: Array<String>) {
    runApplication<SpringWebmvcApplication>(*args)
}
