package com.vacuumlabs.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringJpaApplication

fun main(args: Array<String>) {
    runApplication<SpringJpaApplication>(*args)
}
