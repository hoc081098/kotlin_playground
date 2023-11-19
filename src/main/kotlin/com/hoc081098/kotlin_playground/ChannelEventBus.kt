package com.hoc081098.kotlin_playground

import kotlinx.coroutines.internal.synchronized as coroutinesSynchronized
import com.hoc081098.kmp.viewmodel.Closeable
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
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
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 *
 */
interface ChannelEvent<out T : ChannelEvent<T>> {
  interface Key<out T : ChannelEvent<T>>

  val key: Key<T>
}

typealias ChannelEventKey<T> = ChannelEvent.Key<T>

@OptIn(InternalCoroutinesApi::class)
private class SynchronizedHashMap<K, V> : HashMap<K, V>() {
  @JvmField
  val lock = SynchronizedObject()

  inline fun <T> synchronized(block: () -> T): T = coroutinesSynchronized(lock, block)
}

private data class Entry(
  val channel: Channel<Any>,
  val isCollecting: Boolean
) {
  override fun toString(): String = "${super.toString()}($channel, $isCollecting)"
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelEventBus : Closeable {
  private val _entryMap = SynchronizedHashMap<ChannelEventKey<*>, Entry>()

  private fun getOrCreateEntry(key: ChannelEventKey<*>): Entry =
    _entryMap.synchronized {
      _entryMap.getOrPut(key) {
        Entry(channel = Channel(capacity = Channel.UNLIMITED), isCollecting = false)
          .also { println("CREATED: $key -> $it") }
      }
    }

  private fun getOrCreateEntryAndMarkAsCollecting(key: ChannelEventKey<*>): Entry =
    _entryMap.synchronized {
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
    _entryMap.synchronized {
      val entry = _entryMap[key]!!
      check(entry.isCollecting) { "only one collector is allowed at a time" }
      _entryMap[key] = entry
        .copy(isCollecting = false)
        .also { println("UPDATED: $key -> $it") }
    }


  /**
   * Throws if there is no entry for [key].
   */
  private fun removeEntry(
    key: ChannelEventKey<*>,
    requireNotCollecting: Boolean,
    requireChannelEmpty: Boolean,
  ): Entry =
    _entryMap.synchronized {
      _entryMap
        .remove(key)!!
        .also {
          if (requireNotCollecting) {
            check(!it.isCollecting) { "$key: only one collector is allowed at a time" }
          }
          if (requireChannelEmpty) {
            check(it.channel.isEmpty) { "$key: the channel is not empty. try to consume all elements before closing" }
          }
        }
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

  fun closeKey(
    key: ChannelEventKey<*>,
    requireNotCollecting: Boolean = true,
    requireChannelEmpty: Boolean = false,
  ): Unit = removeEntry(
    key = key,
    requireNotCollecting = requireNotCollecting,
    requireChannelEmpty = requireChannelEmpty
  )
    .channel
    .close()
    .let { }

  override fun close() {
    _entryMap.synchronized {
      _entryMap.forEach { (_, v) -> v.channel.close() }
      _entryMap.clear()
      println("CLOSE: forceCloseAll")
    }
  }
}

data class DemoEvent(val i: Int) : ChannelEvent<DemoEvent> {
  override val key get() = Key

  companion object Key : ChannelEventKey<DemoEvent> {
    override fun toString() = "DemoEvent.Key"
  }
}

data class Demo2Event(val i: Int) : ChannelEvent<Demo2Event> {
  override val key get() = Key

  companion object Key : ChannelEventKey<Demo2Event> {
    override fun toString() = "Demo2Event.Key"
  }
}

fun main(): Unit = runBlocking {
  val bus = ChannelEventBus()
  val d = CompletableDeferred<Unit>()
  val d2 = CompletableDeferred<Unit>()

  bus.send(DemoEvent(1))
  bus.send(Demo2Event(1))
  bus.send(DemoEvent(2))
  bus.send(Demo2Event(2))

  val j = launch {
    bus.receiveAsFlow(DemoEvent).collect {
      println("[1 receive] $it")
      if (it.i == 4) {
        d.complete(Unit)
      }
    }
  }
  val j2 = launch {
    bus.receiveAsFlow(Demo2Event).collect {
      println("[2 receive] $it")
      if (it.i == 4) {
        d2.complete(Unit)
      }
    }
  }

  bus.send(DemoEvent(3))
  bus.send(Demo2Event(3))
  bus.send(Demo2Event(4))
  bus.send(DemoEvent(4))

  d.await()
  d2.await()
  j.cancelAndJoin()
  j2.cancelAndJoin()

  bus.closeKey(DemoEvent)
  bus.closeKey(Demo2Event)

  delay(1000)
  bus.send(DemoEvent(5))
  bus.send(DemoEvent(6))

  runCatching { bus.closeKey(DemoEvent, requireChannelEmpty = true) }
    .onFailure { it.printStackTrace() }

  launch {
    bus.receiveAsFlow(DemoEvent).collect {
      println("[3 receive] $it")
      if (it.i == 6) {
        cancel()
      }
    }
  }
}
