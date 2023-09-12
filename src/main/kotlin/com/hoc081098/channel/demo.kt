package com.hoc081098.channel

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val channel = Channel<Int>(
        capacity = 3,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    launch {
        for (i in 1..10) {
            println("--> $i")
            channel.send(i)
            delay(10)
            println("<-- $i")
        }
        channel.close()
    }

    launch {
        for (message in channel) {
            println("<<Received>> $message")
            delay(1000)
            println()
        }
        println("DONE")
    }
}