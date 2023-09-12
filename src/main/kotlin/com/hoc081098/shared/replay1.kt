package com.hoc081098.shared

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val sharedFlow = MutableSharedFlow<Int>(
        replay = 1,
    )

    sharedFlow
        .onEach { println("[>>>]: $it") }
        .launchIn(this)

    delay(100)
    sharedFlow.emit(1)

    delay(100)
    sharedFlow.emit(2)

    delay(100)
    sharedFlow.emit(3)

    sharedFlow
        .onEach { println("[###]: $it") }
        .launchIn(this)


    delay(100)
    sharedFlow.emit(4)

    delay(100)
    sharedFlow.emit(5)

    delay(100)
    cancel()
}