package com.example.plugins

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.properties.Delegates

fun Application.configureStockExchange() {
    val client = HttpClient(CIO)
    val stockExchangeUrl = environment.config.property("stockExchange.url").getString()
    val port = environment.config.property("stockExchange.port").getString()
    val stockExchangeFullUrl = "http://$stockExchangeUrl:$port"
    stockExchange = StockExchange(client, stockExchangeFullUrl)
}

var Application.stockExchange: StockExchange by Delegates.notNull()

@Serializable
data class TickerData(val tickerPrice: Double, val count: Int)

class StockExchange(private val client: HttpClient, private val url: String) {
    suspend fun getTickerData(ticker: String): TickerData {
        return Json.decodeFromString(client.get("$url/get-ticker?tickerName=$ticker").body())
    }

    suspend fun buyStocks(ticker: String, count: Int): Unit {
        client.post("$url/buy-stocks?tickerName=$ticker&count=$count")
    }

    suspend fun sellStocks(ticker: String, count: Int): Unit {
        client.post("$url/sell-stocks?tickerName=$ticker&count=$count")
    }
}
