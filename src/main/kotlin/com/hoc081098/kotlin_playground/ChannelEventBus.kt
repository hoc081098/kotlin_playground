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
import kotlinx.coroutines.channels.getOrElse
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

sealed class ChannelEventBusException(cause: Throwable?, message: String?) : RuntimeException(message, cause) {
  class FailedToSendEvent(
    val event: ChannelEvent<*>,
    cause: Throwable?,
  ) : ChannelEventBusException(cause, "Failed to send event: $event")

  class FlowAlreadyCollected(
    val key: ChannelEventKey<*>,
  ) : ChannelEventBusException(null, "Flow by key=$key is already collected")
}

/**
 * ## Multi-key, multi-producer, single-consumer event bus backed by [Channel]s.
 *
 * - This bus is thread-safe to be used by multiple threads.
 *
 * - [ChannelEvent.Key] will be used to identify a bus for a specific type of events.
 *   Each bus has a [Channel] to send events to and a [Flow] to receive events from.
 *
 * - The [Channel] is unbounded [Channel.UNLIMITED].
 *   The [Flow] is cold and only one collector is allowed at a time.
 *   This make sure all events are consumed.
 *
 * ## Basic usage:
 * ```kotlin
 * // Create your event type
 * data class AwesomeEvent(val payload: Int) : ChannelEvent<AwesomeEvent> {
 *   override val key get() = Key
 *   companion object Key : ChannelEventKey<AwesomeEvent>
 * }
 *
 * // Create your bus instance
 * val bus = ChannelEventBus()
 *
 * // Send events to the bus
 * bus.send(AwesomeEvent(1))
 * bus.send(AwesomeEvent(2))
 * bus.send(AwesomeEvent(3))
 *
 * // Receive events from the bus
 * bus.receiveAsFlow(AwesomeEvent)
 *   .collect { e: AwesomeEvent -> println(e) }
 * ```
 */
sealed interface ChannelEventBus : Closeable {
  /**
   * Send [event] to the bus identified by [ChannelEvent.key].
   *
   * @throws ChannelEventBusException.FailedToSendEvent if failed to send the event.
   */
  fun <E : ChannelEvent<E>> send(event: E)

  /**
   * Receive events from the bus identified by [ChannelEvent.key].
   *
   * The returned [Flow] is cold and only one collector is allowed at a time.
   * This make sure all events are consumed.
   *
   * If you want to collect the flow multiple times, you must share the flow,
   * or must cancel the previous collection before collecting again with a new one.
   *
   * @throws ChannelEventBusException.FlowAlreadyCollected if collecting the flow is already collected by another collector.
   */
  fun <T : ChannelEvent<T>> receiveAsFlow(key: ChannelEventKey<T>): Flow<T>

  fun closeKey(
    key: ChannelEventKey<*>,
    requireNotCollecting: Boolean = true,
    requireChannelEmpty: Boolean = false,
    requireExists: Boolean = true,
  )
}

/**
 * Create a new [ChannelEventBus] instance.
 *
 * @param logger a [ChannelEventBusLogger] to log events.
 */
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
        if (existing.isCollecting) {
          throw ChannelEventBusException.FlowAlreadyCollected(key)
        }

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
      _entryMap[key] = _entryMap[key]!!
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
    requireExists: Boolean,
  ): Entry? =
    _entryMap.synchronized {
      val removed = _entryMap.remove(key)

      if (requireExists) {
        checkNotNull(removed) { "$key: no entry found" }
      }

      removed?.also {
        if (requireNotCollecting) {
          check(!it.isCollecting) { "$key: only one collector is allowed at a time" }
        }
        if (requireChannelEmpty) {
          check(it.channel.isEmpty) { "$key: the channel is not empty. try to consume all elements before closing" }
        }
        logger?.onClosed(key, this)
      }
    }

  // ---------------------------------------------------------------------------------------------

  override fun <E : ChannelEvent<E>> send(event: E) = getOrCreateEntry(event.key)
    .channel
    .trySend(event)
    .getOrElse { throw ChannelEventBusException.FailedToSendEvent(event, it) }

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
    requireExists: Boolean,
  ) {
    removeEntry(
      key = key,
      requireNotCollecting = requireNotCollecting,
      requireChannelEmpty = requireChannelEmpty,
      requireExists = requireExists,
    )?.channel?.close()
  }

  override fun close() {
    _entryMap.synchronized {
      val keys = logger?.let { _entryMap.keys.toSet() }

      _entryMap.forEach { (_, v) -> v.channel.close() }
      _entryMap.clear()

      logger?.onClosedAll(keys!!, this)
    }
  }
}

// ------------------------------------------- PLAYGROUND --------------------------------------------------

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

  runCatching { bus.closeKey(DemoEvent, requireChannelEmpty = true, requireExists = false) }
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
