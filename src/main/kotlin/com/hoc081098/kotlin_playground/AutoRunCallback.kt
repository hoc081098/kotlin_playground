package com.hoc081098.kotlin_playground

import com.hoc081098.flowext.mapToUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

typealias AutoRunCallback<T> = Resolver.() -> T

class Resolver {
  private val scope = CoroutineScope(Dispatchers.Unconfined + Job())
  
  internal val onChangeFlow = MutableSharedFlow<Unit>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  private val flows = LinkedHashSet<StateFlow<Any?>>()
  
  @Volatile
  private var job: Job? = null
  
  internal fun <T> track(data: StateFlow<T>) {
    check(job === null) { "Frozen!" }
    flows.add(data)
  }
  
  internal fun start() {
    job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
      flows
        .merge()
        .conflate()
        .mapToUnit()
        .collect(onChangeFlow::emit)
    }
  }
  
  internal fun stop() {
    job?.cancel()
    job = null
  }
  
  internal fun depsValue() = Any()
  
  val <T> StateFlow<T>.reactiveValue: T get() = this@Resolver[this]
}

operator fun <T> Resolver.get(data: StateFlow<T>): T {
  track(data)
  return data.value
}

fun <T> derived(observer: AutoRunCallback<T>): StateFlow<T> =
  DerivedStateFlow(observer = observer)

private class DerivedStateFlow<T>(
  private val observer: AutoRunCallback<T>
) : StateFlow<T> {
  private val resolver = Resolver()
  private val refCount = AtomicInteger(0)
  
  private val cache = AtomicReference<Pair<T, Any?>?>(null)
  
  override val replayCache: List<T> get() = listOf(value)
  override val value: T
    get() = cache.updateAndGet { pair ->
      var p = pair
      if (p != null) {
        if (p.second != resolver.depsValue()) {
          val v = observer(resolver)
          val deps = resolver.depsValue()
          p = v to deps
          println("diff deps")
        } else {
          println("cache")
        }
      }
      if (p == null) {
        println("call")
        val v = observer(resolver)
        val deps = resolver.depsValue()
        p = v to deps
      }
      p
    }!!.first
  
  override suspend fun collect(collector: FlowCollector<T>): Nothing {
    try {
      start()
      
      resolver
        .onChangeFlow
        .map { this.value }
        .distinctUntilChanged()
        .collect(collector)
      
      error("?")
    } finally {
      stop()
    }
  }
  
  private fun start() {
    if (refCount.getAndIncrement() == 0) {
      this.value
      resolver.start()
    }
  }
  
  private fun stop() {
    if (refCount.decrementAndGet() == 0) {
      resolver.stop()
    }
  }
}

fun main() = runBlocking {
  val f1 = MutableStateFlow(1)
  val f2 = MutableStateFlow("hello")
  val f3 = MutableStateFlow(4)
  
  var cond = false
  
  val result = derived {
    println("Call $cond")
    if (cond) {
      Triple(
        f3.reactiveValue,
        get(f2),
        get(f1),
      )
    } else {
      Triple(
        get(f1),
        get(f2),
        f3.reactiveValue,
      )
    }
  }
  
  val j = result.onEach { println("---> $it") }.launchIn(this)
  
  delay(1)
  cond = true
  f1.value = 2
  f2.value = "3"
  val j2 = result.onEach { println("---> W $it") }.launchIn(this)
  delay(100)
  f3.value = 99
  f3.value = 97
  delay(1000)
  
  j.cancel()
  j2.cancel()
  
}
