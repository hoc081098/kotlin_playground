package com.hoc081098.kotlin_playground.solivagant

import androidx.compose.runtime.Immutable
import com.hoc081098.channeleventbus.ChannelEvent
import com.hoc081098.channeleventbus.ChannelEventKey

@Immutable
data class SecondResultToFirst(val number: Int) : ChannelEvent<SecondResultToFirst> {
  override val key = Key

  companion object Key : ChannelEventKey<SecondResultToFirst>(SecondResultToFirst::class)
}
