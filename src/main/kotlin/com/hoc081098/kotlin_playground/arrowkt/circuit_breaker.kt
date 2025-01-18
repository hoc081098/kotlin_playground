package com.hoc081098.kotlin_playground.arrowkt

import arrow.core.Either
import arrow.resilience.CircuitBreaker
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay

@ExperimentalTime
suspend fun main(): Unit {
  val circuitBreaker = CircuitBreaker(
    openingStrategy = CircuitBreaker.OpeningStrategy.Count(2),
    resetTimeout = 2.seconds,
    exponentialBackoffFactor = 1.2,
    maxResetTimeout = 60.seconds,
  )

  // normal operation
  circuitBreaker.protectOrThrow { "I am in Closed: ${circuitBreaker.state()}" }.also(::println)

  // simulate service getting overloaded
  Either.catch {
    circuitBreaker.protectOrThrow { throw RuntimeException("Service overloaded") }
  }.also(::println)
  Either.catch {
    circuitBreaker.protectOrThrow { throw RuntimeException("Service overloaded") }
  }.also(::println)
  circuitBreaker.protectEither { }
    .also { println("I am Open and short-circuit with ${it}. ${circuitBreaker.state()}") }

  println(">>> state should be Open: ${circuitBreaker.state()}")
  // simulate reset timeout
  println("Service recovering . . .").also { delay(2500) }
  println(">>> state should be HalfOpen: ${circuitBreaker.state()}")

  // simulate test request success
  circuitBreaker.protectOrThrow {
    "I am running test-request in HalfOpen: ${circuitBreaker.state()}"
  }.also(::println)
  println("I am back to normal state closed ${circuitBreaker.state()}")
}
