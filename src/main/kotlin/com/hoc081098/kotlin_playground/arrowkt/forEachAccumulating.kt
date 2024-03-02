package com.hoc081098.kotlin_playground.arrowkt

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.raise.forEachAccumulating
import arrow.core.raise.mapOrAccumulate

context(Raise<String>)
fun Int.isPositive(): Unit = if (this > 0) Unit else raise("$this is not positive")

context(Raise<String>)
fun String.toPositiveInt(): Int = ensureNotNull(toIntOrNull()) { "$this is not a number" }
  .apply { isPositive() }

fun main() {
  println(
    either {
      forEachAccumulating((-10..10)) { it.isPositive() }
    }
  )
  println(
    either {
      forEachAccumulating((1..10)) { it.isPositive() }
    }
  )

  println("-".repeat(80))

  println(
    either<NonEmptyList<String>, List<Int>> {
      mapOrAccumulate(listOf("1", "2", "3")) { it.toPositiveInt() }
    }
  )
  println(
    either<NonEmptyList<String>, List<Int>> {
      mapOrAccumulate(listOf("-@", "#", "-3")) { it.toPositiveInt() }
    }
  )
  println(
    either<NonEmptyList<String>, List<Int>> {
      mapOrAccumulate(listOf("-@", "#", "3")) { it.toPositiveInt() }
    }
  )
  println(
    either<NonEmptyList<String>, List<Int>> {
      mapOrAccumulate(listOf("-1", "2", "3")) { it.toPositiveInt() }
    }
  )
  println(
    either<NonEmptyList<String>, List<Int>> {
      mapOrAccumulate(listOf("-1", "-2", "-3")) { it.toPositiveInt() }
    }
  )
}
