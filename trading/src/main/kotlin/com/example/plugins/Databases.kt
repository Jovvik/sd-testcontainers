package com.example.plugins

import io.ktor.server.application.*
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.properties.Delegates

data class User(val money: BigDecimal, val stocks: Map<String, Int>)

@Serializable
data class StockData(val tickerName: String, val price: Double, val count: Int)

fun Application.configureDatabases() {
    users = mutableMapOf()
}

var Application.users: MutableMap<String, User> by Delegates.notNull()
