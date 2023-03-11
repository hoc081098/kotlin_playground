package com.hoc081098.kotlin_playground

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.random.Random

private class DemoException : RuntimeException()

private class GetDataRequest {
  private val executor = Executors.newSingleThreadExecutor()
  private var future: Future<Any?>? = null
  private var onCancel: (() -> Unit)? = null

  @Synchronized
  fun start(
    block: (Result<Int>) -> Unit,
    onCancel: () -> Unit
  ) {
    println("Started")

    this.onCancel = onCancel
    future = executor.submit(Callable {
      Thread.sleep(2_000)
      block(
        if (Random.nextBoolean()) {
          Result.success(Random.nextInt(0, 1001))
        } else {
          Result.failure(DemoException())
        }
      )
    })
  }

  @Synchronized
  fun cancel() = runCatching {
    future?.cancel(true)
    future = null
    executor.shutdown()

    onCancel?.invoke()
    onCancel = null
    println("Cancelled")
  }
}

suspend fun getData(): Int {
  val request = GetDataRequest()

  return suspendCancellableCoroutine { cont ->
    request.start(
      block = { result ->
        if (cont.isActive) {
          cont.resumeWith(result)
        }
      },
      onCancel = cont::cancel
    )

    cont.invokeOnCancellation { request.cancel() }
  }
}

fun main() = runBlocking<Unit> {
  val job = launch {
    try {
      println("Result: ${getData()}")
    } catch (e: Exception) {
      println("Error: $e")
      if (e is CancellationException) {
        throw e
      }
    }
  }

  delay(1_000)
  job.cancelAndJoin()
  println("Done")
}