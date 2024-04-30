package com.vacuumlabs.example.kafka

import com.vacuumlabs.example.db.MessageEntity
import com.vacuumlabs.example.db.MessageRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.function.Consumer

@Configuration
class KafkaConfiguration {
    @Bean("message-saver")
    fun messageSaver(messageRepository: MessageRepository) =
        Consumer<TransactionDto> { message ->
            messageRepository.save(MessageEntity(id = null, message = message.description ?: ""))
        }
}
