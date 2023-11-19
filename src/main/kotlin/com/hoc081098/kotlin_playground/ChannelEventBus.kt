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
import kotlinx.coroutines.withTimeout

// ------------------------------------ PUBLIC API ------------------------------------

/**
 *
 */
interface ChannelEvent<out T : ChannelEvent<T>> {
  interface Key<out T : ChannelEvent<T>>

  val key: Key<T>
}

typealias ChannelEventKey<T> = ChannelEvent.Key<T>

interface ChannelEventBusLogger {
  fun onCreated(key: ChannelEventKey<*>, bus: ChannelEventBus)

  fun onStartCollection(key: ChannelEventKey<*>, bus: ChannelEventBus)

  fun onStopCollection(key: ChannelEventKey<*>, bus: ChannelEventBus)

  fun onClosed(key: ChannelEventKey<*>, bus: ChannelEventBus)

  fun onClosedAll(keys: Set<ChannelEventKey<*>>, bus: ChannelEventBus)
}

sealed interface ChannelEventBus : Closeable {
  fun <E : ChannelEvent<E>> send(event: E)

  fun <T : ChannelEvent<T>> receiveAsFlow(key: ChannelEventKey<T>): Flow<T>

  fun closeKey(
    key: ChannelEventKey<*>,
    requireNotCollecting: Boolean = true,
    requireChannelEmpty: Boolean = false,
  )
}

fun ChannelEventBus(logger: ChannelEventBusLogger? = null): ChannelEventBus = ChannelEventBusImpl(logger)

object ConsoleChannelEventBusLogger : ChannelEventBusLogger {
  override fun onCreated(key: ChannelEventKey<*>, bus: ChannelEventBus) =
    println("[$bus] onCreated: key=$key")

  override fun onStartCollection(key: ChannelEventKey<*>, bus: ChannelEventBus) =
    println("[$bus] onStartCollection: key=$key")

  override fun onStopCollection(key: ChannelEventKey<*>, bus: ChannelEventBus) =
    println("[$bus] onStopCollection: key=$key")

  override fun onClosed(key: ChannelEventKey<*>, bus: ChannelEventBus) =
    println("[$bus] onClosed: key=$key")

  override fun onClosedAll(keys: Set<ChannelEventKey<*>>, bus: ChannelEventBus) =
    println("[$bus] onClosedAll: keys=$keys")
}

// ------------------------------------ INTERNAL ------------------------------------

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
private class ChannelEventBusImpl(
  @JvmField val logger: ChannelEventBusLogger?
) : ChannelEventBus {
  private val _entryMap = SynchronizedHashMap<ChannelEventKey<*>, Entry>()

  private fun getOrCreateEntry(key: ChannelEventKey<*>): Entry =
    _entryMap.synchronized {
      _entryMap.getOrPut(key) {
        Entry(channel = Channel(capacity = Channel.UNLIMITED), isCollecting = false)
          .also { logger?.onCreated(key, this) }
      }
    }

  private fun getOrCreateEntryAndMarkAsCollecting(key: ChannelEventKey<*>): Entry =
    _entryMap.synchronized {
      val existing = _entryMap[key]
      if (existing === null) {
        Entry(channel = Channel(capacity = Channel.UNLIMITED), isCollecting = true)
          .also { _entryMap[key] = it }
          .also { logger?.onCreated(key, this) }
      } else {
        check(!existing.isCollecting) { "only one collector is allowed at a time" }

        existing.copy(isCollecting = true)
          .also { _entryMap[key] = it }
          .also { logger?.onStartCollection(key, this) }
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
        .also { logger?.onStopCollection(key, this) }
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
      checkNotNull(
        _entryMap
          .remove(key)
      ) { "$key: no entry found" }
        .also {
          if (requireNotCollecting) {
            check(!it.isCollecting) { "$key: only one collector is allowed at a time" }
          }
          if (requireChannelEmpty) {
            check(it.channel.isEmpty) { "$key: the channel is not empty. try to consume all elements before closing" }
          }
        }
        .also { logger?.onClosed(key, this) }
    }

  // ---------------------------------------------------------------------------------------------

  override fun <E : ChannelEvent<E>> send(event: E): Unit = getOrCreateEntry(event.key).channel
    .trySend(event)
    .let { }

  @Suppress("UNCHECKED_CAST")
  override fun <T : ChannelEvent<T>> receiveAsFlow(key: ChannelEventKey<T>): Flow<T> = flow {
    getOrCreateEntryAndMarkAsCollecting(key)
      .channel
      .receiveAsFlow()
      .map { it as T }
      .let { emitAll(it) }
  }.onCompletion { markAsNotCollecting(key) }

  override fun closeKey(
    key: ChannelEventKey<*>,
    requireNotCollecting: Boolean,
    requireChannelEmpty: Boolean,
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
      val keys = logger?.let { _entryMap.keys.toSet() }

      _entryMap.forEach { (_, v) -> v.channel.close() }
      _entryMap.clear()

      logger?.onClosedAll(keys!!, this)
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
  val bus = ChannelEventBus(logger = ConsoleChannelEventBusLogger)
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

  bus.send(DemoEvent(100))
  bus.send(DemoEvent(200))
  bus.close()

  runCatching { bus.closeKey(DemoEvent, requireChannelEmpty = true) }
    .onFailure { it.printStackTrace() }

  launch {
    withTimeout(5000) {
      bus.receiveAsFlow(DemoEvent).collect {
        println("[3 receive] $it")
        if (it.i == 6) {
          cancel()
        }
      }
    }
  }
}
