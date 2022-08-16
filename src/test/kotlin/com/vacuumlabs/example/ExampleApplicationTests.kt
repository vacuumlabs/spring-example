package com.vacuumlabs.example

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.DEFAULT, printOnlyOnFailure = false)
class ExampleApplicationTests @Autowired constructor(
    val mockMvc: MockMvc,
) {
    @Test
    fun contextLoads() {
    }

    @Test
    fun `get messages`() {
        mockMvc.get("/messages").andExpect {
            status {
                isOk()
            }
            content {
                json("[]")
            }
        }
    }

    @Test
    fun `health endpoint`() {
        mockMvc.get("/actuator/health").andExpect { status { isOk() } }
    }
}
