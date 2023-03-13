package com.example

import com.example.plugins.configureDatabases
import com.example.plugins.configureRouting
import com.example.plugins.configureStockExchange
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class ApplicationTest {
    companion object {
        @Container
        val stockExchange = GenericContainer(DockerImageName.parse("stock-exchange:0.0.1"))
            .withExposedPorts(8080)
    }

    private suspend fun setUp() {
        val client = HttpClient(CIO)
        client.post("http://localhost:${stockExchange.firstMappedPort}/add-ticker?tickerName=AAPL&tickerPrice=20")
    }

    @Test
    fun testRoot() {
        runBlocking { setUp() }
        testApplication {
            environment {
                config = MapApplicationConfig(
                    "stockExchange.port" to stockExchange.firstMappedPort.toString(),
                    "stockExchange.url" to stockExchange.getHost(),
                )
            }
            application {
                configureStockExchange()
                configureDatabases()
                configureRouting()
            }
            client.post("/add-user?userId=user").apply {
                assertEquals(HttpStatusCode.OK, status)
            }
            client.post("/add-money?userId=user&money=1000").apply {
                assertEquals(HttpStatusCode.OK, status)
            }
            client.get("/money?userId=user").apply {
                assertEquals(1000.0, body<String>().toDouble(), 1e-5)
            }
            client.get("/stocks?userId=user").apply {
                assertEquals("[]", body<String>())
            }
            client.post("/buy-stocks?userId=user&tickerName=AAPL&count=1").apply {
                println(body<String>())
                assertEquals(HttpStatusCode.OK, status)
            }
            val money = client.get("/money?userId=user").body<String>().toDouble()
            assertEquals(money, 1000.0)
            delay(1000)
            val newMoney = client.get("/money?userId=user").body<String>().toDouble()
            assertNotEquals(money, newMoney)
        }
    }
}
