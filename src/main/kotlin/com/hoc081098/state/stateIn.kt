package com.hoc081098.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

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
    val intStateFlow = upstreamFlow().stateIn(
        scope = scope,
        started = SharingStarted.Lazily,
        initialValue = -1,
    )
    println("Current value: ${intStateFlow.value}")

    delay(1000)
    println("First subscriber")
    intStateFlow
        .onEach { println("[---]: $it") }
        .launchIn(scope)

    delay(2000)
}
