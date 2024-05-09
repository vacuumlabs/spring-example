package com.vacuumlabs.example.kafka

import org.springframework.stereotype.Service

@Service
class TransactionRepository() {
    private val transactions = mutableListOf<TransactionDto>()

    fun save(transaction: TransactionDto) {
        transactions.add(transaction)
    }

    fun findAll(): List<TransactionDto> {
        return transactions
    }
}
