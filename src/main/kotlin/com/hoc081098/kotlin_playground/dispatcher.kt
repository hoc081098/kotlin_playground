package com.hoc081098.kotlin_playground

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

private suspend fun currentDispatcher() = currentCoroutineContext()[ContinuationInterceptor]

fun <K, V> Map<K, V>.formatted() = entries.joinToString(
  separator = ",\n",
  prefix = "{\n",
  postfix = "}",
) { (k, v) -> "        \"$k\": \"$v\"" }

object FakeCoroutineDispatcher : CoroutineDispatcher() {
  override fun isDispatchNeeded(context: CoroutineContext): Boolean {
    println("Call ${Thread.currentThread().name}")
    return false
  }

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    TODO("Not yet implemented")
  }
}

object TwoDispatcher : CoroutineDispatcher() {
  private val i = AtomicInteger()
  override fun dispatch(context: CoroutineContext, block: Runnable) {
    thread(name = "Thread ${i.incrementAndGet()}") {
      block.run()
    }
  }
}

fun main() = runBlocking {
  flowOf(1, 2, 3, 4, 5)
    .map { delay(100); it }
    .onEach {
      println(
        "[UP]: " +
            mapOf(
              "it" to it,
              "currentDispatcher" to currentDispatcher(),
              "currentThread" to Thread.currentThread().name,
            ).formatted()
      )
    }
    .flowOn(TwoDispatcher)
    .onEach {
      println(
        "[DOWN]: " +
            mapOf(
              "it" to it,
              "currentDispatcher" to currentDispatcher(),
              "currentThread" to Thread.currentThread().name,
            ).formatted()
      )
      println("-".repeat(80))
    }
    .launchIn(CoroutineScope(FakeCoroutineDispatcher))
    .join()

  withContext(FakeCoroutineDispatcher) {
    println("Unconfined: ${Thread.currentThread().name} ${currentDispatcher()}")
  }
}
