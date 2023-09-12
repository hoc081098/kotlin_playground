package com.hoc081098.shared

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private fun upstreamFlow() = flow {
    println("[upstreamFlow] Start collecting")

    delay(1_000)
    emit(1)

    delay(100)
    emit(2)

    delay(100)
    emit(3)

    delay(100)
    emit(4)
}.onCompletion { println("[upstreamFlow] Completed $it") }

fun main() = runBlocking {
    val scope = CoroutineScope(Dispatchers.Default)
    val sharedFlow = upstreamFlow().shareIn(
        scope = scope,
        started = SharingStarted.Lazily,
        replay = 0,
    )
    println("After shareIn")

    delay(1_000)
    println("Subscribe")
    sharedFlow
        .onEach { println("[---]: $it") }
        .launchIn(scope)

    delay(100)
    sharedFlow
        .onEach { println("[###]: $it") }
        .launchIn(scope)

    delay(2_000)
    scope.cancel()
}