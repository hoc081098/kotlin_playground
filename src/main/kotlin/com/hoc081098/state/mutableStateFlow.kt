package com.hoc081098.state

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val stateFlow = MutableStateFlow(0)

    stateFlow
        .onEach { println("[>>>]: $it") }
        .launchIn(this)


    delay(100)
    stateFlow.emit(1)
    println(stateFlow.value)

    delay(100)
    stateFlow.value = 1
    println(stateFlow.value)

    delay(100)
    stateFlow.tryEmit(1)
    println(stateFlow.value)

    delay(100)
    stateFlow.update { 1 }
    println(stateFlow.value)

    delay(0)
    stateFlow.value = 2
    println(stateFlow.value)


    Unit
}