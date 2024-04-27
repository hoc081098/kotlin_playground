package com.hoc081098.kotlin_playground.arrowkt

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.forEachAccumulating
import arrow.core.raise.mapOrAccumulate

@JvmInline
value class Age private constructor(val value: Int) {
  companion object {
    context(Raise<InvalidAge>) // Use `context` to raise an error.
    fun from(value: Int): Age =
      if (value in 0..150) Age(value)
      else raise(InvalidAge(value))
  }
}

@JvmInline
value class InvalidAge(val invalidValue: Int)

fun main() {
  val ints = listOf(-10, -2, 0, 20, 500)

  // use `forEachAccumulating` to accumulate all errors in a `NonEmptyList`
  val either1: Either<NonEmptyList<InvalidAge>, Unit> = either {
    forEachAccumulating(ints) { Age.from(it) }
  }

  // use `forEach` to stop at the first error
  val either2: Either<InvalidAge, Unit> = either {
    ints.forEach { Age.from(it) }
  }

  println(either1)
  println(either2)

  val either3: Either<NonEmptyList<InvalidAge>, List<Age>> = either { mapOrAccumulate(ints) { Age.from(it) } }
  println(either3)
}
