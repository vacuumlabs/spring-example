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
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

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
    fun kafkaPublish(@Valid @RequestBody transactionDto: TransactionDto) {
        streamBridge.send("transaction-sender-out-0", transactionDto)
    }

    @GetMapping("/messages")
    fun getMessages(): Iterable<MessageEntity> = messageRepository.findAll()
}

@Configuration
class KafkaConfiguration {
    @Bean
    fun messageSaver(messageRepository: MessageRepository) = Consumer<TransactionDto> { message ->
        if (message.accountNumber != "ACC-123456") {
            throw java.lang.IllegalArgumentException("Account doesn't exist: ${message.accountNumber}")
        }
        messageRepository.save(MessageEntity(id = null, message = message.description ?: ""))
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
