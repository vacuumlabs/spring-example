package com.vacuumlabs.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.vacuumlabs.example.db.MessageEntity
import com.vacuumlabs.example.db.MessageRepository
import com.vacuumlabs.example.kafka.TransactionDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.KafkaContainer
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
        val kafkaContainer: KafkaContainer,
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

        @Test
        @DirtiesContext
        fun `new transaction - nonexistent account number`() {
            postNewTransaction(
                TransactionDto(1, "ACC-654321", BigDecimal(1000), "Test transaction"),
            ).andExpect { status { isOk() } }

            val record = awaitRecord("error.test-topic.message-saver")
            val exceptionMessage = record?.headers()?.lastHeader("x-exception-message")?.value()?.decodeToString()
            assertThat(exceptionMessage).endsWith("Account doesn't exist: ACC-654321")
            assertThat(messageRepository.findAll()).isEmpty()
        }

        private fun postNewTransaction(transactionDto: TransactionDto): ResultActionsDsl {
            return mockMvc.post("/transactions") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(transactionDto)
            }
        }

        private fun awaitRecord(topic: String): ConsumerRecord<String, String> {
            val props =
                KafkaTestUtils.consumerProps(
                    kafkaContainer.bootstrapServers,
                    this.javaClass.name,
                    "false",
                )
            return KafkaConsumer<String, String>(props).use { consumer ->
                consumer.assign(listOf(TopicPartition(topic, 0)))
                KafkaTestUtils.getSingleRecord(consumer, topic, java.time.Duration.ofSeconds(10))
            }
        }
    }
