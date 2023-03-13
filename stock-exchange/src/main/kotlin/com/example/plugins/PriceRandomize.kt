package com.example.plugins

import io.ktor.server.application.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.example.plugins.tickers
import java.math.BigDecimal

class Scheduler(private val task: Runnable) {
    private val executor = Executors.newScheduledThreadPool(1)!!

    fun scheduleExecution(every: Every) {

        val taskWrapper = Runnable {
            task.run()
        }

        executor.scheduleWithFixedDelay(taskWrapper, every.n, every.n, every.unit)
    }


    fun stop() {
        executor.shutdown()
        try {
            executor.awaitTermination(1, TimeUnit.HOURS)
        } catch (e: InterruptedException) {
        }
    }
}

data class Every(val n: Long, val unit: TimeUnit)

fun Application.configureRandomize() {
    val scheduler = Scheduler {
        tickers.forEach { it.value.tickerPrice = it.value.tickerPrice.add(BigDecimal(Math.random() * 10 - 5)) }
    }
    scheduler.scheduleExecution(Every(1, TimeUnit.SECONDS))
    environment.monitor.subscribe(ApplicationStopped) {
        scheduler.stop()
    }
}