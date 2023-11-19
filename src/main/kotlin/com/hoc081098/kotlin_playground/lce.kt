package com.hoc081098.kotlin_playground

import arrow.core.Either
import com.hoc081098.flowext.concatWith
import com.hoc081098.flowext.flowFromSuspend
import com.hoc081098.flowext.interval
import com.hoc081098.flowext.startWith
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking

/**
 * Loading | Content | Error
 */
sealed interface LCE<out E, out T> {
  data object Loading : LCE<Nothing, Nothing>
  data class Content<out T>(val content: T) : LCE<Nothing, T>
  data class Error<out E>(val error: E) : LCE<E, Nothing>
}

// ------------------------------------ LCE EXTENSIONS ------------------------------------

private typealias LCEErrorMapper<E> = suspend (throwable: Throwable) -> E

@JvmField
@PublishedApi
internal val IdentityErrorMapper: LCEErrorMapper<Throwable> = { it }

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

// ------------------------------------ FLOW + LCE ------------------------------------

@Suppress("NOTHING_TO_INLINE")
inline fun <T> Flow<T>.toLCEFlow(): Flow<LCE<Throwable, T>> = toLCEFlow(IdentityErrorMapper)

fun <E, T> Flow<T>.toLCEFlow(errorMapper: LCEErrorMapper<E>): Flow<LCE<E, T>> =
  map<T, LCE<E, T>> { LCE.Content(it) }
    .startWith(LCE.Loading)
    .catch { emit(LCE.Error(errorMapper(it))) }

fun <T> lceFlowFromSuspend(function: suspend () -> T): Flow<LCE<Throwable, T>> =
  flowFromSuspend(function).toLCEFlow()

fun <E, T> lceFlowFromSuspend(
  errorMapper: LCEErrorMapper<E>,
  function: suspend () -> T
): Flow<LCE<E, T>> = flowFromSuspend(function).toLCEFlow(errorMapper)

// ------------------------------------ EITHER <~> LCE ------------------------------------

/**
 * Convert [Either] to [LCE].
 *
 * When [Either] is `null`, return [LCE.Loading].
 * Otherwise, return [LCE.Error] if [Either] is [Either.Left],
 * or [LCE.Content] if [Either] is [Either.Right].
 */
fun <L, R> Either<L, R>?.asLCE(): LCE<L, R> {
  this ?: return LCE.Loading
  return fold(
    ifLeft = { LCE.Error(it) },
    ifRight = { LCE.Content(it) }
  )
}

/**
 * Convert [LCE] to [Either].
 *
 * When [LCE] is [LCE.Loading], return `null`.
 * Otherwise, return [Either.Left] if [LCE] is [LCE.Error],
 * or [Either.Right] if [LCE] is [LCE.Content].
 */
fun <L, R> LCE<L, R>.asEither(): Either<L, R>? = when (this) {
  is LCE.Loading -> null
  is LCE.Content -> Either.Right(content)
  is LCE.Error -> Either.Left(error)
}

/**
 * Convert [Flow] of [Either] to [Flow] of [LCE].
 * @see asLCE
 */
fun <L, R> Flow<Either<L, R>?>.asLCEFlow(): Flow<LCE<L, R>> = map { it.asLCE() }

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

  lceFlowFromSuspend { delay(100); "Hello" }
    .onEach(::println)
    .collect()

  lceFlowFromSuspend<String> { error("Fake") }
    .onEach(::println)
    .collect()

  println("-".repeat(80))

  lceFlowFromSuspend({ "Error: $it" }, { delay(100); "Hello" })
    .onEach(::println)
    .collect()

  lceFlowFromSuspend({ "Error: $it" }, { error("Fake") as String })
    .onEach(::println)
    .collect()

  println("-".repeat(80))

  val lce: LCE<Any, String> = LCE.Content("Hello")
  if (lce.isContent()) {
    println(lce.content)
  }

  println("-".repeat(80))

  val lceF: Flow<LCE<String, Int>> = flowOf(
    null,
    Either.Right(1),
    Either.Left("Error"),
  ).asLCEFlow()
  lceF.collect(::println)
}
