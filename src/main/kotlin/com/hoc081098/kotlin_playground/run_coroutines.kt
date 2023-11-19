package com.hoc081098.kotlin_playground

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.suspendCancellableCoroutine

@JvmField
val ScheduledExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors())

suspend fun myDelay(duration: Duration) = suspendCancellableCoroutine { cont ->
  val future = ScheduledExecutor.schedule(
    { cont.resume(Unit) },
    duration.inWholeNanoseconds,
    TimeUnit.NANOSECONDS
  )
  cont.invokeOnCancellation { if (it != null) future.cancel(false) }
}

suspend fun mySuspendFunction(): Int {
  myDelay(1.seconds).also { println("Done 1") }
  myDelay(2.seconds).also { println("Done 2") }
  return 42
}

fun main() {
  ::mySuspendFunction
    .startCoroutine(Continuation(EmptyCoroutineContext) {
      ScheduledExecutor.shutdownNow()
      println("Done result=$it")
    })
}
