package com.hoc081098.kotlin_playground

import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

interface ChannelEvent<out T : ChannelEvent<T>> {
  interface Key<out T : ChannelEvent<T>>

  val key: Key<T>
}

typealias ChannelEventKey<T> = ChannelEvent.Key<T>

class ChannelEventBus {
  private data class Entry(
    val channel: Channel<Any>,
    val isCollecting: Boolean
  )

  // @GuardedBy("_entryMap")
  private val _entryMap = hashMapOf<ChannelEventKey<*>, Entry>()

  private fun getOrCreateEntry(key: ChannelEventKey<*>): Entry =
    synchronized(_entryMap) {
      _entryMap.getOrPut(key) {
        Entry(channel = Channel(capacity = Channel.UNLIMITED), isCollecting = false)
          .also { println("CREATED: $key -> $it") }
      }
    }

  private fun getOrCreateEntryAndMarkAsCollecting(key: ChannelEventKey<*>): Entry =
    synchronized(_entryMap) {
      val existing = _entryMap[key]
      if (existing === null) {
        Entry(channel = Channel(capacity = Channel.UNLIMITED), isCollecting = true)
          .also { _entryMap[key] = it }
          .also { println("CREATED: $key -> $it") }
      } else {
        check(!existing.isCollecting) { "only one collector is allowed at a time" }

        existing.copy(isCollecting = true)
          .also { _entryMap[key] = it }
          .also { println("UPDATED: $key -> $it") }
      }
    }


  /**
   * Throws if there is no entry for [key].
   */
  private fun markAsNotCollecting(key: ChannelEventKey<*>): Unit =
    synchronized(_entryMap) {
      val entry = _entryMap[key]!!
      check(entry.isCollecting) { "only one collector is allowed at a time" }
      _entryMap[key] = entry
        .copy(isCollecting = false)
        .also { println("UPDATED: $key -> $it") }
    }


  /**
   * Throws if there is no entry for [key].
   */
  private fun removeEntry(key: ChannelEventKey<*>): Entry =
    synchronized(_entryMap) {
      _entryMap
        .remove(key)!!
        .also { check(!it.isCollecting) { "only one collector is allowed at a time" } }
        .also { println("REMOVED: $key -> $it") }
    }

  // ---------------------------------------------------------------------------------------------

  fun <E : ChannelEvent<E>> send(event: E): Unit = getOrCreateEntry(event.key).channel.trySend(event)
    .let { res ->
      println("LOG: Sent $event -> $res")
      _entryMap.forEach { (k, v) ->
        println("LOG: $k -> $v")
      }
      println("-".repeat(80))
    }


  @Suppress("UNCHECKED_CAST")
  fun <T : ChannelEvent<T>> receiveAsFlow(key: ChannelEventKey<T>): Flow<T> = flow {
    getOrCreateEntryAndMarkAsCollecting(key)
      .channel
      .receiveAsFlow()
      .map { it as T }
      .let { emitAll(it) }
  }.onCompletion { markAsNotCollecting(key) }

  fun close(key: ChannelEventKey<*>): Unit = removeEntry(key).channel.close().let { }

  fun forceCloseAll() {
    synchronized(_entryMap) {
      _entryMap.forEach { (_, v) -> v.channel.close() }
      _entryMap.clear()
      println("CLOSE: forceCloseAll")
    }
  }
}


data class DemoEvent(val i: Int) : ChannelEvent<DemoEvent> {
  override val key get() = Key

  companion object Key : ChannelEventKey<DemoEvent>
}

fun main(): Unit = runBlocking {
  val bus = ChannelEventBus()
  val d = CompletableDeferred<Unit>()

  bus.send(DemoEvent(1))
  bus.send(DemoEvent(2))

  val j = launch {
    bus.receiveAsFlow(DemoEvent).collect {
      println(">>>: $it")
      if (it.i == 4) {
        d.complete(Unit)
      }
    }
  }

  bus.send(DemoEvent(3))
  bus.send(DemoEvent(4))

  d.await()
  j.cancelAndJoin()
  bus.close(DemoEvent)

  delay(1000)
  bus.send(DemoEvent(5))
  bus.send(DemoEvent(6))
  launch {
    bus.receiveAsFlow(DemoEvent).collect {
      println(">>>: $it")
      if (it.i == 6) {
        cancel()
      }
    }
  }
}
