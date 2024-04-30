package com.vacuumlabs.example.db

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
data class MessageEntity(
    @Id
    @GeneratedValue
    val id: Long?,
    val message: String,
)
