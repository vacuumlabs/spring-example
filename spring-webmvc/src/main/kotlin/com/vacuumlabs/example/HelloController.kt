package com.vacuumlabs.example

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HelloController {
    val logger = LoggerFactory.getLogger(javaClass)!!

    @GetMapping
    fun hello(): String {
        logger.info("Hello endpoint called")
        return "Hello, World!"
    }
}
