package com.vacuumlabs.example

import io.github.oshai.kotlinlogging.withLoggingContext
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

@RestController
class HelloController(
    private val restTemplate: RestTemplate,
) {
    val logger = LoggerFactory.getLogger(javaClass)!!

    @GetMapping
    fun hello(): String {
        withLoggingContext("userId" to "123") {
            logger.info("Hello endpoint called")
        }

        this.restTemplate.getForObject<String>("http://localhost:8080/bye")

        return "Hello, World!"
    }

    @GetMapping("/bye")
    fun bye(): String {
        logger.info("Bye endpoint called")
        return "Bye, World!"
    }
}
