package com.hoc081098.kotlin_playground

import com.ensody.reactivestate.derived
import com.ensody.reactivestate.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take

suspend fun main() {
  val f1 = MutableStateFlow(1)
  val f2 = MutableStateFlow("hello")
  val f3 = MutableStateFlow(4)
  
  var cond = false
  
  val result = derived {
    println("Call $cond")
    if (cond) {
      Triple(
        get(f3),
        get(f2),
        get(f1),
      )
    } else {
      Triple(
        get(f1),
        get(f2),
        get(f3),
      )
    }
  }
  
  val j = result.onEach { println("---> $it") }.launchIn(CoroutineScope(Dispatchers.Unconfined))
  
  delay(1)
  cond = true
  f1.value = 2
  f2.value = "3"
  val j2 = result.onEach { println("---> W $it") }.launchIn(CoroutineScope(Dispatchers.Unconfined))
  delay(100)
  f3.value = 99
  f3.value = 97
  delay(1000)
  
  j.cancel()
  j2.cancel()
  
  
  result.value
  
  result.take(2).collect()
  
  result.value
}