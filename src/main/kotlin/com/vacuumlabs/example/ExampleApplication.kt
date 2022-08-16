package com.vacuumlabs.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@SpringBootApplication
class ExampleApplication

fun main(args: Array<String>) {
    runApplication<ExampleApplication>(*args)
}

@RestController
class MessageController(
    val messageRepository: MessageRepository
) {
    @GetMapping("/messages")
    fun getMessages(): Iterable<MessageEntity> = messageRepository.findAll()
}

@Repository
interface MessageRepository : CrudRepository<MessageEntity, Long>

@Entity
data class MessageEntity(
    @Id
    @GeneratedValue
    val id: Long?,
    val message: String,
)
