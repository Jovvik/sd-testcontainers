package com.example.plugins

import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import com.example.plugins.tickers
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.util.pipeline.*

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
    install(ContentNegotiation) {
        json()
    }
    routing {
        post("/add-ticker") {
            val tickerName = getParameter("tickerName", "No ticker name provided") ?: return@post
            val tickerPrice = getParameter("tickerPrice", "No ticker price provided") ?: return@post
            if (this@configureRouting.tickers.containsKey(tickerName)) {
                respondError("Ticker already exists")
                return@post
            }
            this@configureRouting.tickers[tickerName] = TickerData(
                tickerPrice = tickerPrice.toBigDecimal(),
                count = 0
            )
            call.respondText("Ticker $tickerName added")
        }
        post("/sell-stocks") {
            val tickerName = getParameter("tickerName", "No ticker name provided") ?: return@post
            val count = getParameter("count", "No count provided") ?: return@post
            val curTickerData = this@configureRouting.tickers[tickerName]
            if (curTickerData == null) {
                respondError("Ticker $tickerName does not exist")
                return@post
            }
            this@configureRouting.tickers[tickerName] = TickerData(
                tickerPrice = curTickerData.tickerPrice,
                count = curTickerData.count + count.toInt()
            )
            call.respondText("Stocks sold")
        }
        get("/get-ticker") {
            val tickerName = getParameter("tickerName", "No ticker name provided") ?: return@get
            val curTickerData = this@configureRouting.tickers[tickerName]
            if (curTickerData == null) {
                respondError("Ticker $tickerName does not exist")
                return@get
            }
            call.respond(
                TickerDataResponse(
                    tickerPrice = curTickerData.tickerPrice.toDouble(),
                    count = curTickerData.count
                )
            )
        }
        post("/buy-stocks") {
            val tickerName = getParameter("tickerName", "No ticker name provided") ?: return@post
            val count = getParameter("count", "No count provided") ?: return@post
            val curTickerData = this@configureRouting.tickers[tickerName]
            if (curTickerData == null) {
                respondError("Ticker $tickerName does not exist")
                return@post
            }
            if (curTickerData.count < count.toInt()) {
                respondError("Not enough stocks in stock")
                return@post
            }
            this@configureRouting.tickers[tickerName] = TickerData(
                tickerPrice = curTickerData.tickerPrice,
                count = curTickerData.count - count.toInt()
            )
            call.respond(
                BuyResponse(
                    tickerPrice = curTickerData.tickerPrice.toDouble(),
                    count = count.toInt()
                )
            )
        }
    }
}