package com.hoc081098.kotlin_playground

import java.time.LocalDate

@ExperimentalStdlibApi
fun main() {
  for (i in 0..<5) {
    println(i) // 0, 1, 2, 3, 4
  }

  val today: LocalDate = LocalDate.now()
  val yesterday = today.minusDays(1)
  val twoDaysAgo: LocalDate = today.minusDays(2)

  println(today in twoDaysAgo..<today) // false
  println(yesterday in twoDaysAgo..<today) // true
}
