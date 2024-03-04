package com.hoc081098.kotlin_playground

import hu.akarnokd.kotlin.flow.takeUntil as akarnokdTakeUntil
import com.hoc081098.flowext.neverFlow
import com.hoc081098.flowext.takeUntil
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

@OptIn(FlowPreview::class)
fun main() = runBlocking {
  flowOf(1)
    .akarnokdTakeUntil(neverFlow())
    .toList()
    .let(::println)

  flowOf(1)
    .takeUntil(neverFlow())
    .toList()
    .let(::println)
}
