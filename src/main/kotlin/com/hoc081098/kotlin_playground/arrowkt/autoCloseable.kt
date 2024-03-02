package com.hoc081098.kotlin_playground.arrowkt

import arrow.autoCloseScope
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class FirstAC : AutoCloseable {
  init {
    println("$this is created")
  }

  override fun close() = println("$this is closed")
  override fun toString() = "[1]"
}

class SecondAC(private val firstAC: FirstAC) : AutoCloseable {
  init {
    println("$this is created")
  }

  override fun close() = println("$this is closed")
  override fun toString() = "[2]"
}

class ThirdAC(private val secondAC: SecondAC, private val firstAC: FirstAC) : AutoCloseable {
  init {
    println("$this is created")
  }

  suspend fun doSomething(): Int {
    println("$this do something")
    delay(1000)
    return Random.nextInt()
  }

  override fun close() = println("$this is closed")
  override fun toString() = "[3]"
}

@OptIn(ExperimentalStdlibApi::class)
fun main(): Unit = runBlocking {
  val r1 = FirstAC().use { f ->
    SecondAC(f).use { s ->
      ThirdAC(s, f).use { t ->
        t.doSomething()
      }
    }
  }

  println("-".repeat(80))

  val r2 = autoCloseScope {
    val first = install(FirstAC())
    val second = install(SecondAC(first))
    val third = install(ThirdAC(second, first))
    third.doSomething()
  }
}
