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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

// ------------------------------------ PUBLIC API ------------------------------------

/**
 * Represents an event that can be sent to a [ChannelEventBus].
 */
interface ChannelEvent<out T : ChannelEvent<T>> {
  interface Key<out T : ChannelEvent<T>>

  /**
   * The key to identify a bus for this type of events.
   */
  val key: Key<T>
}

typealias ChannelEventKey<T> = ChannelEvent.Key<T>

/**
 * Logger for [ChannelEventBus].
 * It is used to log events of [ChannelEventBus].
 */
interface ChannelEventBusLogger {
  /**
   * Called when a bus associated with [key] is created.
   * @see [ChannelEventBus.send]
   */
  fun onCreated(key: ChannelEventKey<*>, bus: ChannelEventBus)

  /**
   * Called when a bus associated with [key] is collecting.
   * @see [ChannelEventBus.receiveAsFlow]
   */
  fun onStartCollection(key: ChannelEventKey<*>, bus: ChannelEventBus)

  /**
   * Called when a bus associated with [key] is stopped collecting.
   * @see [ChannelEventBus.receiveAsFlow]
   */
  fun onStopCollection(key: ChannelEventKey<*>, bus: ChannelEventBus)

  /**
   * Called when a bus associated with [key] is closed.
   * @see [ChannelEventBus.closeKey]
   */
  fun onClosed(key: ChannelEventKey<*>, bus: ChannelEventBus)

  /**
   * Called when all buses are closed.
   * @see [ChannelEventBus.close]
   */
  fun onClosedAll(keys: Set<ChannelEventKey<*>>, bus: ChannelEventBus)
}

/**
 * Represents an exception thrown by [ChannelEventBus].
 */
sealed class ChannelEventBusException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
  abstract val key: ChannelEventKey<*>

  /**
   * Represents an exception thrown when failed to send an event to a bus.
   *
   * @param event the event that failed to send.
   */
  class FailedToSendEvent(
    val event: ChannelEvent<*>,
    cause: Throwable?,
  ) : ChannelEventBusException("Failed to send event: $event", cause) {
    override val key get() = event.key
  }

  /**
   * Represents an exception thrown when trying to collect a flow that is already collected by another collector.
   */
  class FlowAlreadyCollected(
    override val key: ChannelEventKey<*>,
  ) : ChannelEventBusException("Flow by key=$key is already collected", null)

  /**
   * Represents an exception thrown when trying to close a bus.
   */
  sealed class CloseException(message: String?, cause: Throwable?) : ChannelEventBusException(message, cause) {
    /**
     * Represents an exception thrown when trying to close a bus that does not exist.
     */
    class BusDoesNotExist(
      override val key: ChannelEventKey<*>,
    ) : CloseException("Bus by key=$key does not exist", null)

    /**
     * Represents an exception thrown when trying to close a bus that is collecting.
     */
    class BusIsCollecting(
      override val key: ChannelEventKey<*>,
    ) : CloseException("Bus by key=$key is collecting, must cancel the collection before closing", null)

    /**
     * Represents an exception thrown when trying to close a bus that is not empty (all events are not consumed completely).
     */
    class BusIsNotEmpty(
      override val key: ChannelEventKey<*>,
    ) : CloseException("Bus by key=$key is not empty, try to consume all elements before closing", null)
  }
}

/**
 * ## Multi-keys, multi-producers, single-consumer event bus backed by [Channel]s.
 *
 * - This bus is thread-safe to be used by multiple threads.
 *   It is safe to send events from multiple threads without any synchronization.
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

  /**
   * Close the bus identified by [key].
   *
   * You can validate the bus before closing by passing [requireNotCollecting], [requireChannelEmpty], [requireExists]
   * - If [requireNotCollecting] is `true`, the bus must not be collecting by any collector before closing.
   * - If [requireChannelEmpty] is `true`, the channel must be empty before closing.
   * - If [requireExists] is `true`, the bus must exist before closing.
   *
   * @param key the key to identify the bus.
   * @param requireNotCollecting require the bus must not be collecting by any collector before closing.
   * If `true` and the bus is collecting, [ChannelEventBusException.CloseException.BusIsCollecting] will be thrown.
   * @param requireChannelEmpty require the channel must be empty before closing.
   * If `true` and the channel is not empty, [ChannelEventBusException.CloseException.BusIsNotEmpty] will be thrown.
   * @param requireExists require the bus must exist before closing.
   * If `true` and the bus does not exist, [ChannelEventBusException.CloseException.BusDoesNotExist] will be thrown.
   *
   * @throws ChannelEventBusException.CloseException if failed to close the bus.
   */
  fun closeKey(
    key: ChannelEventKey<*>,
    requireNotCollecting: Boolean = true,
    requireChannelEmpty: Boolean = false,
    requireExists: Boolean = true,
  )

  /**
   * Close all buses without any validation.
   */
  override fun close()
}

/**
 * Create a new [ChannelEventBus] instance.
 *
 * @param logger a [ChannelEventBusLogger] to log events.
 */
fun ChannelEventBus(logger: ChannelEventBusLogger? = null): ChannelEventBus = ChannelEventBusImpl(logger)

/**
 * The [ChannelEventBusLogger] that simply prints events to the console via [println].
 */
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

/**
 * A bus contains a [Channel] and a flag to indicate whether the channel is collecting or not.
 */
private data class Bus(
  val channel: Channel<Any>,
  val isCollecting: Boolean
) {
  override fun toString(): String = "${super.toString()}($channel, $isCollecting)"
}

@OptIn(ExperimentalCoroutinesApi::class)
private class ChannelEventBusImpl(
  @JvmField val logger: ChannelEventBusLogger?
) : ChannelEventBus {
  /**
   * Guarded by [SynchronizedHashMap.lock].
   */
  private val _busMap = SynchronizedHashMap<ChannelEventKey<*>, Bus>()

  private fun getOrCreateBus(key: ChannelEventKey<*>): Bus =
    _busMap.synchronized {
      _busMap.getOrPut(key) {
        Bus(channel = Channel(capacity = Channel.UNLIMITED), isCollecting = false)
          .also { logger?.onCreated(key, this) }
      }
    }

  private fun getOrCreateBusAndMarkAsCollecting(key: ChannelEventKey<*>): Bus =
    _busMap.synchronized {
      val existing = _busMap[key]
      if (existing === null) {
        Bus(channel = Channel(capacity = Channel.UNLIMITED), isCollecting = true)
          .also { _busMap[key] = it }
          .also { logger?.onCreated(key, this) }
      } else {
        if (existing.isCollecting) {
          throw ChannelEventBusException.FlowAlreadyCollected(key)
        }

        existing.copy(isCollecting = true)
          .also { _busMap[key] = it }
          .also { logger?.onStartCollection(key, this) }
      }
    }


  /**
   * Throws if there is no bus associated with [key].
   */
  private fun markAsNotCollecting(key: ChannelEventKey<*>): Unit =
    _busMap.synchronized {
      _busMap[key] = _busMap[key]!!
        .copy(isCollecting = false)
        .also { logger?.onStopCollection(key, this) }
    }


  /**
   * @throws ChannelEventBusException.CloseException
   */
  private fun removeBus(
    key: ChannelEventKey<*>,
    requireNotCollecting: Boolean,
    requireChannelEmpty: Boolean,
    requireExists: Boolean,
  ): Bus? = _busMap.synchronized {
    val removed = _busMap.remove(key)

    if (requireExists) {
      if (removed === null) {
        throw ChannelEventBusException.CloseException.BusDoesNotExist(key)
      }
    }

    removed?.also {
      if (requireNotCollecting && it.isCollecting) {
        throw ChannelEventBusException.CloseException.BusIsCollecting(key)
      }
      if (requireChannelEmpty && !it.channel.isEmpty) {
        throw ChannelEventBusException.CloseException.BusIsNotEmpty(key)
      }
      logger?.onClosed(key, this)
    }
  }

  // ---------------------------------------------------------------------------------------------

  override fun <E : ChannelEvent<E>> send(event: E) = getOrCreateBus(event.key)
    .channel
    .trySend(event)
    .getOrElse { throw ChannelEventBusException.FailedToSendEvent(event, it) }

  @Suppress("UNCHECKED_CAST")
  override fun <T : ChannelEvent<T>> receiveAsFlow(key: ChannelEventKey<T>): Flow<T> = flow {
    try {
      getOrCreateBusAndMarkAsCollecting(key)
        .channel
        .receiveAsFlow()
        .map { it as T }
        .let { emitAll(it) }
    } catch (e: Throwable) {
      markAsNotCollecting(key)
      throw e
    }

    // Normal completion
    markAsNotCollecting(key)
  }

  override fun closeKey(
    key: ChannelEventKey<*>,
    requireNotCollecting: Boolean,
    requireChannelEmpty: Boolean,
    requireExists: Boolean,
  ) {
    removeBus(
      key = key,
      requireNotCollecting = requireNotCollecting,
      requireChannelEmpty = requireChannelEmpty,
      requireExists = requireExists,
    )?.channel?.close()
  }

  override fun close() {
    _busMap.synchronized {
      val keys = logger?.let { _busMap.keys.toSet() }

      _busMap.forEach { (_, v) -> v.channel.close() }
      _busMap.clear()

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
    bus.receiveAsFlow(DemoEvent).first().let { println("taken $it") }
    println("OK")
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
