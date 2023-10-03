package com.hoc081098.kotlin_playground

import com.hoc081098.flowext.concatWith
import com.hoc081098.flowext.flowFromSuspend
import com.hoc081098.flowext.interval
import com.hoc081098.flowext.startWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed interface LCE<out E, out T> {
  data object Loading : LCE<Nothing, Nothing>
  data class Content<out T>(val content: T) : LCE<Nothing, T>
  data class Error<out E>(val error: E) : LCE<E, Nothing>
}

@OptIn(ExperimentalContracts::class)
@Suppress("NOTHING_TO_INLINE")
inline fun <E, T> LCE<E, T>.contentOrNull(): T? {
  contract {
    returnsNotNull() implies (this@contentOrNull is LCE.Content<T>)
  }

  return when (this) {
    is LCE.Content -> content
    else -> null
  }
}

@OptIn(ExperimentalContracts::class)
@Suppress("NOTHING_TO_INLINE")
inline fun <E, T> LCE<E, T>.errorOrNull(): E? {
  contract {
    returnsNotNull() implies (this@errorOrNull is LCE.Error<E>)
  }

  return when (this) {
    is LCE.Error -> error
    else -> null
  }
}

@OptIn(ExperimentalContracts::class)
@Suppress("NOTHING_TO_INLINE")
inline fun <E, T> LCE<E, T>.isContent(): Boolean {
  contract {
    returns(true) implies (this@isContent is LCE.Content<T>)
  }

  return this is LCE.Content
}

@OptIn(ExperimentalContracts::class)
@Suppress("NOTHING_TO_INLINE")
inline fun <E, T> LCE<E, T>.isLoading(): Boolean {
  contract {
    returns(true) implies (this@isLoading is LCE.Loading)
  }

  return this is LCE.Loading
}

@OptIn(ExperimentalContracts::class)
@Suppress("NOTHING_TO_INLINE")
inline fun <E, T> LCE<E, T>.isError(): Boolean {
  contract {
    returns(true) implies (this@isError is LCE.Error<E>)
  }
  return this is LCE.Error
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Flow<T>.toLCEFlow(): Flow<LCE<Throwable, T>> = toLCEFlow { it }

fun <E, T> Flow<T>.toLCEFlow(errorMapper: suspend (Throwable) -> E): Flow<LCE<E, T>> =
  map<T, LCE<E, T>> { LCE.Content(it) }
    .startWith(LCE.Loading)
    .catch { emit(LCE.Error(errorMapper(it))) }

fun <T> lceFlowOf(function: suspend () -> T) =
  flowFromSuspend(function).toLCEFlow()

fun <E, T> lceFlowOf(errorMapper: suspend (Throwable) -> E, function: suspend () -> T) =
  flowFromSuspend(function)
    .toLCEFlow(errorMapper)

fun main() = runBlocking<Unit> {
  interval(0, 100)
    .take(4)
    .concatWith(flow { error("Broken!") })
    .toLCEFlow()
    .collect(::println)

  println("-".repeat(80))

  interval(0, 100)
    .take(4)
    .concatWith(flow { error("Broken!") })
    .toLCEFlow { "Error: $it" }
    .onEach(::println)
    .collect()

  println("-".repeat(80))

  lceFlowOf { delay(100); "Hello" }
    .onEach(::println)
    .collect()

  lceFlowOf<String> { error("Fake") }
    .onEach(::println)
    .collect()

  println("-".repeat(80))

  lceFlowOf({ "Error: $it" }, { delay(100); "Hello" })
    .onEach(::println)
    .collect()

  lceFlowOf({ "Error: $it" }, { error("Fake") as String })
    .onEach(::println)
    .collect()

  println("-".repeat(80))

  val lce: LCE<Any, String> = LCE.Content("Hello")
  if (lce.isContent()) {
    println(lce.content)
  }
}