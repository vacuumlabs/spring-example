package com.vacuumlabs.example.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.function.Consumer

@Configuration
class KafkaConfiguration {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Bean("message-saver")
    fun messageSaver(transactionRepository: TransactionRepository) =
        Consumer<TransactionDto> { transaction ->
            if (transaction.accountNumber != "ACC-123456") {
                throw java.lang.IllegalArgumentException("Account doesn't exist: ${transaction.accountNumber}")
            }
            transactionRepository.save(transaction)
        }
}
