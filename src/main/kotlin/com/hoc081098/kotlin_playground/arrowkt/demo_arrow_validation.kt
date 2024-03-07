package com.hoc081098.kotlin_playground.arrowkt

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.forEachAccumulating

@JvmInline
value class Age(val value: Int) {
  companion object {
    context(Raise<InvalidAge>)
    fun from(value: Int): Age =
      if (value in 0..150) Age(value)
      else raise(InvalidAge(value))
  }
}

data class InvalidAge(val value: Int)

fun main() {
  val hasInvalidAges = listOf(-10, -2, 0, 10, 150, 200)

  val either1: Either<NonEmptyList<InvalidAge>, Unit> = either {
    forEachAccumulating(hasInvalidAges) { Age.from(it) }
  }
  val either2: Either<InvalidAge, Unit> = either {
    hasInvalidAges.forEach { Age.from(it) }
  }

  println(either1)
  println(either2)
}
