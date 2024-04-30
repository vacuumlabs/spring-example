package com.vacuumlabs.example.db

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MessageController(
    val messageRepository: MessageRepository
) {
    @GetMapping("/messages")
    fun getMessages(): Iterable<MessageEntity> = messageRepository.findAll()
}
