package com.hoc081098.kotlin_playground

import com.hoc081098.flowext.flowFromSuspend
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun main() = runBlocking {
  val flow1 = flowFromSuspend {
    println(">>> 1 ${Thread.currentThread()}")
    delay(100)
    1
  }.flowOn(newSingleThreadContext("flow1-dispatcher"))

  val flow2 = flowFromSuspend {
    println(">>> 2 ${Thread.currentThread()}")
    delay(100)
    2
  }.flowOn(newSingleThreadContext("flow2-dispatcher"))

  combine(flow1, flow2) { a, b -> println(">>> combine ${Thread.currentThread()}"); a + b }
    .flowOn(newSingleThreadContext("combine-dispatcher"))
    .collect(::println)
}
