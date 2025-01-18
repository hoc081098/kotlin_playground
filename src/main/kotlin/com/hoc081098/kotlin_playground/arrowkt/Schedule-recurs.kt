package com.hoc081098.kotlin_playground.arrowkt

import arrow.resilience.Schedule
import arrow.resilience.retry
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
  val recurs: Schedule<Throwable, Long> = Schedule.recurs<Throwable>(3)

  recurs.retry {
    println(">>> calling...")
    throw RuntimeException("Error")
  }
}
