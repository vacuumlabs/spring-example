package com.vacuumlabs.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.vacuumlabs.example.db.MessageEntity
import com.vacuumlabs.example.db.MessageRepository
import com.vacuumlabs.example.kafka.TransactionDto
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.Duration

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.DEFAULT, printOnlyOnFailure = false)
@Import(ContainerConfiguration::class)
class KafkaIntegrationTests
    @Autowired
    constructor(
        val mockMvc: MockMvc,
        val objectMapper: ObjectMapper,
        val messageRepository: MessageRepository,
    ) {
        @Test
        @DirtiesContext
        fun `new transaction`() {
            postNewTransaction(
                TransactionDto(1, "ACC-123456", BigDecimal(1000), "Test transaction"),
            ).andExpect { status { isOk() } }

            Awaitility.await().pollDelay(Duration.ofMillis(100)).atMost(Duration.ofSeconds(5)).until {
                messageRepository.findAll().toList().isNotEmpty()
            }
            assertThat(messageRepository.findAll()).isEqualTo(listOf(MessageEntity(1, "Test transaction")))
        }

        private fun postNewTransaction(transactionDto: TransactionDto): ResultActionsDsl {
            return mockMvc.post("/transactions") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(transactionDto)
            }
        }
    }
