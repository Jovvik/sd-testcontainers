package com.example.plugins

import io.ktor.server.application.*
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.properties.Delegates

data class TickerData(
    var tickerPrice: BigDecimal,
    val count: Int
)

@Serializable
data class TickerDataResponse(
    val tickerPrice: Double,
    val count: Int
)

@Serializable
data class BuyResponse(
    val tickerPrice: Double,
    val count: Int,
)

fun Application.configureDatabase() {
    tickers = mutableMapOf()
}

var Application.tickers: MutableMap<String, TickerData> by Delegates.notNull()