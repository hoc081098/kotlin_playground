package com.hoc081098.kotlin_playground

import com.hoc081098.flowext.concatWith
import com.hoc081098.flowext.interval
import com.hoc081098.flowext.startWith
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

sealed interface LCE<out E, out T> {
  data object Loading : LCE<Nothing, Nothing>
  data class Content<out T>(val content: T) : LCE<Nothing, T>
  data class Error<out E>(val error: E) : LCE<E, Nothing>
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Flow<T>.toLCEFlow(): Flow<LCE<Throwable, T>> = toLCEFlow { it }

inline fun <T, E> Flow<T>.toLCEFlow(crossinline errorMapper: suspend (Throwable) -> E): Flow<LCE<E, T>> =
  map<T, LCE<E, T>> { LCE.Content(it) }
    .startWith(LCE.Loading)
    .catch { emit(LCE.Error(errorMapper(it))) }

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
}