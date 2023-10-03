package com.hoc081098.kotlin_playground

import com.hoc081098.flowext.concatWith
import com.hoc081098.flowext.flowFromSuspend
import com.hoc081098.flowext.interval
import com.hoc081098.flowext.startWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

sealed interface LCE<out E, out T> {
  data object Loading : LCE<Nothing, Nothing>
  data class Content<out T>(val content: T) : LCE<Nothing, T>
  data class Error<out E>(val error: E) : LCE<E, Nothing>
}


inline fun <E, T> LCE<E, T>.contentOrNull(): T? =
  when (this) {
    is LCE.Content -> content
    else -> null
  }

inline fun <E, T> LCE<E, T>.errorOrNull(): E? =
  when (this) {
    is LCE.Error -> error
    else -> null
  }

inline val <E, T> LCE<E, T>.isContent get() = this is LCE.Content
inline val <E, T> LCE<E, T>.isLoading get() = this is LCE.Loading
inline val <E, T> LCE<E, T>.isError get() = this is LCE.Error

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

object NormalObject

fun main() = runBlocking<Unit> {
  // operator == of data object compares two object by type, not reference.
  // that means: `other is LCE.Loading`.
  // normal object compares by reference, that means: `other === LCE.Loading`.
  val newLoading = LCE.Loading::class.java
    .getDeclaredConstructor()
    .apply { isAccessible = true }
    .newInstance()
  println("data object: === should be false -> ${newLoading === LCE.Loading}")
  println("data object: == should be true ${newLoading == LCE.Loading}")
  println("-".repeat(80))

  val newNormalObject = NormalObject::class.java
    .getDeclaredConstructor()
    .apply { isAccessible = true }
    .newInstance()
  println("normal object: === should be false -> ${newNormalObject === NormalObject}")
  println("normal object: == should be false -> ${newNormalObject == NormalObject}")
  println("-".repeat(80))

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
}