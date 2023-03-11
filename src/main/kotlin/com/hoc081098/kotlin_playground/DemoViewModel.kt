package com.hoc081098.kotlin_playground

import com.hoc081098.flowext.interval
import com.hoc081098.kmp.viewmodel.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext

class DemoViewModel : ViewModel() {
  private val _state: MutableStateFlow<LCE<Throwable, String>> = MutableStateFlow(LCE.Loading)
  val state: StateFlow<LCE<Throwable, String>> = _state.asStateFlow()

  init {
    interval(0, 100)
      .map { it.toString() }
      .toLCEFlow()
      .onEach { _state.value = it }
      .launchIn(viewModelScope)
  }
}

fun main(): Unit = runBlocking {
  val demoViewModel = DemoViewModel()
  val job = demoViewModel.state
    .onEach { println(it) }
    .launchIn(CoroutineScope(EmptyCoroutineContext))

  delay(1000)

  job.cancel()
  demoViewModel.clear()

  println("Done")
}