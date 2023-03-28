package com.hoc081098.kotlin_playground

import com.hoc081098.flowext.FlowExtPreview
import com.hoc081098.flowext.groupBy
import com.hoc081098.flowext.range
import com.hoc081098.flowext.takeUntil
import com.hoc081098.flowext.timer
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit.MILLISECONDS

@OptIn(FlowExtPreview::class)
fun main() = runBlocking {
  Flowable
    .generate<String, Int>(
      { 0 },
      BiFunction { state, emitter ->
        if (state < 10) {
          emitter.onNext("Value: $state")
        } else {
          emitter.onComplete()
        }
        state + 1
      }
    )
    .materialize()
    .subscribe { println("---> $it") }

  Thread.sleep(10_000)

  println("=====================================")

  range(1, 10)
    .onEach { delay(400) }
    .groupBy { it % 3 }
    .flatMapMerge {
      it.takeUntil(
        timer(
          Unit,
          200,
        )
      )
    }
    .flowOn(Dispatchers.IO)
    .collect { println(">>>> $it") }

  println("=====================================")

  Flowable
    .range(1, 10)
    .concatMap {
      Flowable
        .just(it)
        .delay(400, MILLISECONDS)
    }
    .groupBy { it % 3 }
    .flatMap { g ->
      g.takeUntil(
        Flowable.timer(
          200,
          MILLISECONDS
        )
      )
    }
    .subscribeOn(
      Schedulers.io()
    )
    .subscribe {
      println(">>> $it")
    }

  Thread.sleep(12_000)
}