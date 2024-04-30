package com.vacuumlabs.example.kafka

import java.math.BigDecimal

data class TransactionDto(
    val priority: Int?,
    val accountNumber: String?,
    val amount: BigDecimal?,
    val description: String?,
)
