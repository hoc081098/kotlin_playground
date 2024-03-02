package com.hoc081098.kotlin_playground.arrowkt

import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.forEachAccumulating

context(Raise<String>)
fun Int.validate(): Unit = if (this > 0) Unit else raise("$this is not positive")

fun main() {
  println(
    either {
      forEachAccumulating((-10..10)) { it.validate() }
    }
  )

  println(
    either {
      forEachAccumulating((1..10)) { it.validate() }
    }
  )
}
