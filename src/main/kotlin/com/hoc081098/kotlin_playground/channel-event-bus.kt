package com.hoc081098.kotlin_playground

import com.hoc081098.channeleventbus.ChannelEvent
import com.hoc081098.channeleventbus.ChannelEventBus
import com.hoc081098.channeleventbus.ChannelEventBusLogger
import com.hoc081098.channeleventbus.ChannelEventKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking

data class AwesomeEvent(val payload: Int) : ChannelEvent<AwesomeEvent> {
  override val key get() = Key

  companion object Key : ChannelEventKey<AwesomeEvent>(AwesomeEvent::class)
}

@OptIn(DelicateCoroutinesApi::class)
fun main(): Unit = runBlocking {
  val dispatcher = newFixedThreadPoolContext(4, "ChannelEventBus-thread")
  val scope = CoroutineScope(dispatcher)
  val channelEventBus = ChannelEventBus(logger = ChannelEventBusLogger.stdout())

  val job = scope.launch {
    repeat(100) {
      scope.launch {
        println("[SENT] >>> Sent $it on ${Thread.currentThread().name}")
        channelEventBus.send(AwesomeEvent(it))
      }.join()

      delay(10)
    }
  }

  scope.launch {
    channelEventBus
      .receiveAsFlow(AwesomeEvent)
      .collect { println("[RECEIVED] <<< $it on ${Thread.currentThread().name}\n") }
  }

  job.join()
  scope.cancel()
  dispatcher.close()
}
