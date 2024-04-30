package com.vacuumlabs.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.function.Consumer
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

@SpringBootApplication
class ExampleApplication

fun main(args: Array<String>) {
    runApplication<ExampleApplication>(*args)
}

@RestController
class ExampleController(
    private val streamBridge: StreamBridge,
    private val messageRepository: MessageRepository,
) {
    @PostMapping("/transactions")
    fun transactionSender(@Valid @RequestBody transactionDto: TransactionDto) {
        streamBridge.send(
            "transaction-sender-out-0",
            TransactionMessage(
                transactionDto.accountNumber!!,
                transactionDto.amount!!,
                transactionDto.description!!,
            )
        )
    }

    @GetMapping("/messages")
    fun getMessages(): Iterable<MessageEntity> = messageRepository.findAll()
}

@Configuration
class KafkaConfiguration {
    @Bean("message-saver")
    fun messageSaver(messageRepository: MessageRepository) = Consumer<TransactionMessage> { message ->
        if (message.accountNumber != "ACC-123456") {
            throw java.lang.IllegalArgumentException("Account doesn't exist: ${message.accountNumber}")
        }
        messageRepository.save(MessageEntity(id = null, message = message.description))
    }
}

data class TransactionDto(
    @field:Min(0)
    @field:Max(10)
    val priority: Int?,

    @field:Pattern(regexp = "ACC-[0-9]{6}")
    val accountNumber: String?,

    @NotNull
    val amount: BigDecimal?,

    @field:NotBlank(message = "Message is mandatory")
    val description: String?,
)

@Repository
interface MessageRepository : CrudRepository<MessageEntity, Long>

@Entity
data class MessageEntity(
    @Id
    @GeneratedValue
    val id: Long?,
    val message: String,
)
