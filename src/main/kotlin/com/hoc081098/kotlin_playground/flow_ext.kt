package com.hoc081098.kotlin_playground

import arrow.atomic.AtomicInt
import com.hoc081098.flowext.FlowExtPreview
import com.hoc081098.flowext.flatMapConcatEager
import com.hoc081098.flowext.flowFromSuspend
import com.hoc081098.flowext.mapToResult
import com.hoc081098.flowext.retryWithExponentialBackoff
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

fun interface Request<T> {
  suspend fun execute(): T
}

@OptIn(
  FlowExtPreview::class,
  ExperimentalCoroutinesApi::class,
  FlowPreview::class,
)
class Processor<T>(
  concurrency: Int = 3,
  dispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val onResult: suspend (Request<T>, Result<T>) -> Unit,
) {
  private val requests = Channel<Request<T>>(Channel.UNLIMITED)
  private val scope = CoroutineScope(dispatcher + SupervisorJob())

  fun enqueue(request: Request<T>) =
    requests.trySendBlocking(request).getOrThrow()

  init {
    requests
      .consumeAsFlow()
      .flatMapConcatEager(concurrency = concurrency) { request ->
        flowFromSuspend { request.execute() }
          .retryWithExponentialBackoff(
            initialDelay = 1.seconds,
            factor = 2.0,
            maxAttempt = 2,
          ) { e ->
            println("[Error] $e -> retrying...")
            true
          }
          .mapToResult()
          .map { request to it }
      }
      .onEach { (request, result) -> onResult(request, result) }
      .launchIn(scope)
  }

  fun close() =
    runBlocking { scope.cancel(); println("${this@Processor} closed") }
}


fun main() = runBlocking {
  val concurrency = 3
  val processor = Processor<Int>(concurrency = concurrency) { request, result ->
    println("<<< Request $request -> $result")
  }

  val count = AtomicInt()

  repeat(20) { index ->
    processor.enqueue {
      count.incrementAndGet().let {
        if (it > concurrency) {
          exitProcess(-1)
        }
      }

      try {
        println(">>> Request $index started")
        delay((500L..1_000L).random())
        if (index % 10 == 0) {
          throw RuntimeException("fake error $index")
        }
        println(">>> Request $index finished")
        index
      } finally {
        count.decrementAndGet().let {
          if (it < 0) {
            exitProcess(-1)
          }
        }
      }
    }
  }

  delay(10_000)
  processor.close()
}
