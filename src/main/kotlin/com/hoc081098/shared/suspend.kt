package com.hoc081098.shared

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.seconds

fun main() = runBlocking {
    val scope = CoroutineScope(Dispatchers.Default)

    val sharedFlow = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    sharedFlow
        .onEach { println("[>>>]: $it") }
        .launchIn(scope)

    sharedFlow
        .onEach {
            println("[###]: $it")
            delay(5_000)
        }
        .launchIn(scope)

    scope.launch {
        repeat(100) {
            println("[>>>> sending]: $it")
            sharedFlow.emit(it)
            println("[<<<< sent]: $it")
        }
    }


    delay(20.seconds)
    scope.cancel()
}