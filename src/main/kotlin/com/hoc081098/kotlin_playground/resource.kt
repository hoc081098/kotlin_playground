package com.hoc081098.kotlin_playground

import arrow.fx.coroutines.asFlow
import arrow.fx.coroutines.resource
import arrow.fx.coroutines.resourceScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
  val res = resource(
    acquire = {
      println("Acquire"); "Hello"
    },
    release = { v, exitCase ->
      println("Release $v $exitCase")
    },
  )
  
  res.use {
    println("1 Before delay")
    delay(1000)
    println("1 After delay")
    println(it)
  }
  
  println("-".repeat(80))
  
  res.use {
    println("2 Before delay")
    delay(1000)
    println("2 After delay")
    println(it)
  }
  
  println("-".repeat(80))
  
  val a = resourceScope {
    val v = res.bind()
    println("3 Before delay")
    delay(1000)
    println("3 After delay")
    println(v)
    
    "${v}_1"
  }
  
  println("Done $a")
  
  println("-".repeat(80))
  
  res
    .asFlow()
    .collect { v ->
      println("4 Before delay")
      delay(1000)
      println("4 After delay")
      println(v)
    }
}