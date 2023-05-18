package com.hoc081098.kotlin_playground

import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking

class EventChannel<T> {
  private val c = Channel<T>(Channel.UNLIMITED)
  
  val f = c.receiveAsFlow()
  
  fun send(value: T) {
    c.trySend(value)
  }
}

suspend fun printCurrentDispatcher(label: String = "") {
  println("[$label] Current dispatcher: ${currentCoroutineContext()[ContinuationInterceptor]}")
}

fun main() = runBlocking {
  printCurrentDispatcher()
  
  val eventChannel = EventChannel<Int>()
  
  launch {
    eventChannel.f
      .flowOn(newSingleThreadContext("GAA"))
      .collect {
        println("collect $it")
        printCurrentDispatcher("collect $it")
      }
  }
  
  eventChannel.send(1)
}