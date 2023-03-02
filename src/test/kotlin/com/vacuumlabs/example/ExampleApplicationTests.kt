package com.vacuumlabs.example

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.awaitility.Duration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.math.BigDecimal

@SpringBootTest(properties = ["management.prometheus.metrics.export.enabled=true"])
@AutoConfigureMockMvc(print = MockMvcPrint.DEFAULT, printOnlyOnFailure = false)
@Testcontainers
class ExampleApplicationTests @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    val messageRepository: MessageRepository,
) {

    companion object {
        @Container
        @JvmStatic
        val dc = DockerComposeContainer(File("docker-compose.yaml"))
            .withOptions("--compatibility")
            .withLocalCompose(true)
            .withOptions("--compatibility")
            .withExposedService("kafka", 9092, Wait.forListeningPort())
            .withExposedService("postgres", 5432, Wait.forListeningPort())
            .withExposedService("schemaregistry", 8081, Wait.forListeningPort())
    }

    @Test
    fun contextLoads() {
    }

    @Test
    @WithMockUser
    fun `get messages`() {
        mockMvc.get("/messages")
            .andExpect {
                status {
                    isOk()
                }
                content {
                    json("[]")
                }
            }
    }

    @Test
    fun `get messages without authentication - invalid`() {
        mockMvc.get("/messages")
            .andExpect {
                status {
                    isUnauthorized()
                }
            }
    }

    @Test
    fun `health endpoint`() {
        mockMvc.get("/actuator/health").andExpect { status { isOk() } }
    }

    @Test
    fun `metrics endpoint`() {
        mockMvc.get("/actuator/prometheus").andExpect { status { isOk() } }
    }

    @Test
    @WithMockUser
    fun `new transaction - invalid`() {
        postNewTransaction(
            TransactionDto(11, null, null, null)
        ).andExpect { status { isBadRequest() } }
    }

    @Test
    @WithMockUser
    @DirtiesContext
    fun `new transaction - valid`() {
        postNewTransaction(
            TransactionDto(1, "ACC-123456", BigDecimal(1000), "Test transaction")
        ).andExpect { status { isOk() } }

        Awaitility.await().pollDelay(Duration.ONE_HUNDRED_MILLISECONDS).atMost(Duration.FIVE_SECONDS).until {
            messageRepository.findAll().toList().isNotEmpty()
        }
        assertThat(messageRepository.findAll()).isEqualTo(listOf(MessageEntity(1, "Test transaction")))
    }

    @Test
    @WithMockUser
    @DirtiesContext
    fun `new transaction - valid, nonexistent account number`() {
        postNewTransaction(
            TransactionDto(1, "ACC-654321", BigDecimal(1000), "Test transaction")
        ).andExpect { status { isOk() } }

        val record = awaitRecord("error.test-topic.message-saver")
        val exceptionMessage = record?.headers()?.lastHeader("x-exception-message")?.value()?.decodeToString()
        assertThat(exceptionMessage).endsWith("Account doesn't exist: ACC-654321")
        assertThat(messageRepository.findAll()).isEmpty()
    }

    @Test
    fun `new transaction without authentication - invalid`() {
        postNewTransaction(
            TransactionDto(1, "ACC-123456", BigDecimal(1000), "Test transaction")
        ).andExpect { status { isUnauthorized() } }
    }

    private fun postNewTransaction(
        transactionDto: TransactionDto
    ): ResultActionsDsl {
        return mockMvc.post("/transactions") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(transactionDto)
            with(csrf())
        }
    }

    private fun awaitRecord(topic: String): ConsumerRecord<String, String> {
        val props = KafkaTestUtils.consumerProps(
            "localhost:9092",
            this.javaClass.name,
            "false",
        )
        return KafkaConsumer<String, String>(props).use { consumer ->
            consumer.assign(listOf(TopicPartition(topic, 0)))
            KafkaTestUtils.getSingleRecord(consumer, topic, java.time.Duration.ofSeconds(10))
        }
    }
}
