package com.hoc081098.kotlin_playground

import com.hoc081098.flowext.withLatestFrom
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun pr(m: Any?) = println("[${Thread.currentThread()}] - $m")

fun main() = runBlocking<Unit> {
  pr("Start")

  launch {
    flowOf(1)
      .withLatestFrom(flowOf(1))
      .collect { pr(it) }
  }

  pr("Done")
}

