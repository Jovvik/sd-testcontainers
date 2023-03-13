package com.example.plugins

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.encodeToJsonElement
import java.math.BigDecimal

private suspend fun PipelineContext<Unit, ApplicationCall>.respondError(error: String) {
    call.respondText(
        error,
        status = HttpStatusCode.BadRequest
    )
}

private suspend fun PipelineContext<Unit, ApplicationCall>.getParameter(name: String, error: String): String? {
    val result = call.request.queryParameters[name]
    if (result == null) {
        respondError(error)
    }
    return result
}

fun Application.configureRouting() {
//    install(ContentNegotiation) {
//        json()
//    }
    routing {
        post("/add-user") {
            val userId = getParameter("userId", "No user id provided") ?: return@post
            this@configureRouting.users[userId] = User(
                money = BigDecimal.ZERO,
                stocks = emptyMap()
            )
            call.respondText("User $userId added")
        }
        post("/add-money") {
            val userId = getParameter("userId", "No user id provided") ?: return@post
            val money = getParameter("money", "No money provided") ?: return@post
            val moneyBigDecimal = try {
                money.toBigDecimal()
            } catch (e: NumberFormatException) {
                respondError("Money is not a number")
                return@post
            }
            if (moneyBigDecimal <= BigDecimal.ZERO) {
                respondError("Added money must be positive")
                return@post
            }
            val curUser = this@configureRouting.users[userId]
            if (curUser == null) {
                respondError("User $userId does not exist")
                return@post
            }
            this@configureRouting.users[userId] = curUser.copy(
                money = curUser.money + moneyBigDecimal
            )
            call.respondText("Money added")
        }
        get("/stocks") {
            val userId = getParameter("userId", "No user id provided") ?: return@get
            val curUser = this@configureRouting.users[userId]
            if (curUser == null) {
                respondError("User $userId does not exist")
                return@get
            }
            call.respond(JsonArray(curUser.stocks.map { (tickerName, count) ->
                val tickerData = this@configureRouting.stockExchange.getTickerData(tickerName)
                Json.encodeToJsonElement(
                    StockData(
                        tickerName = tickerName,
                        price = tickerData.tickerPrice,
                        count = count
                    )
                )
            }).toString())
        }
        get("/money") {
            val userId = getParameter("userId", "No user id provided") ?: return@get
            val curUser = this@configureRouting.users[userId]
            if (curUser == null) {
                respondError("User $userId does not exist")
                return@get
            }
            val money = curUser.money.toDouble() + curUser.stocks.map { (tickerName, count) ->
                this@configureRouting.stockExchange.getTickerData(tickerName).tickerPrice * count
            }.sum()
            call.respondText(money.toString())
        }
        post("/buy-stocks") {
            val userId = getParameter("userId", "No user id provided") ?: return@post
            val tickerName = getParameter("tickerName", "No ticker name provided") ?: return@post
            val count = getParameter("count", "No count provided") ?: return@post
            val countInt = try {
                count.toInt()
            } catch (e: NumberFormatException) {
                respondError("Count is not a number")
                return@post
            }
            if (countInt <= 0) {
                respondError("Count must be positive")
                return@post
            }
            val curUser = this@configureRouting.users[userId]
            if (curUser == null) {
                respondError("User $userId does not exist")
                return@post
            }
            val tickerData = this@configureRouting.stockExchange.getTickerData(tickerName)
            val price = tickerData.tickerPrice
            val totalPrice = price * countInt
            if (curUser.money < totalPrice.toBigDecimal()) {
                respondError("Not enough money")
                return@post
            }
            this@configureRouting.stockExchange.buyStocks(tickerName, countInt)
            val curCount = curUser.stocks[tickerName] ?: 0
            this@configureRouting.users[userId] = curUser.copy(
                money = curUser.money - totalPrice.toBigDecimal(),
                stocks = curUser.stocks + (tickerName to (curCount + countInt))
            )
            call.respondText("Stocks bought")
        }
        post("/sell-stocks") {
            val userId = getParameter("userId", "No user id provided") ?: return@post
            val tickerName = getParameter("tickerName", "No ticker name provided") ?: return@post
            val count = getParameter("count", "No count provided") ?: return@post
            val countInt = try {
                count.toInt()
            } catch (e: NumberFormatException) {
                respondError("Count is not a number")
                return@post
            }
            if (countInt <= 0) {
                respondError("Count must be positive")
                return@post
            }
            val curUser = this@configureRouting.users[userId]
            if (curUser == null) {
                respondError("User $userId does not exist")
                return@post
            }
            val curCount = curUser.stocks[tickerName] ?: 0
            if (curCount < countInt) {
                respondError("Not enough stocks")
                return@post
            }
            this@configureRouting.stockExchange.sellStocks(tickerName, countInt)
            val tickerData =
                this@configureRouting.stockExchange.getTickerData(tickerName)
            val price = tickerData.tickerPrice
            val totalPrice = price * countInt
            this@configureRouting.users[userId] = curUser.copy(
                money = curUser.money + totalPrice.toBigDecimal(),
                stocks = curUser.stocks + (tickerName to (curCount - countInt))
            )
            call.respondText("Stocks sold")
        }
    }
}
