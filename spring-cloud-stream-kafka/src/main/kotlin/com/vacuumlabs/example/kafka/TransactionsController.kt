package com.vacuumlabs.example.kafka

import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TransactionsController(
    private val streamBridge: StreamBridge,
) {
    @PostMapping("/transactions")
    fun transactionSender(
        @RequestBody transactionDto: TransactionDto,
    ) {
        streamBridge.send("transaction-sender-out-0", transactionDto)
    }
}
