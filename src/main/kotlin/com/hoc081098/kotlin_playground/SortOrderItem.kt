package com.hoc081098.kotlin_playground

data class Item1(val value: Int)

data class Item2(val value: String)

data object Item3

enum class SortOrderItem {
  ITEM1,
  ITEM2,
  ITEM3;
}

private val orderMapByKClass = mapOf(
  Item1::class to SortOrderItem.ITEM1,
  Item2::class to SortOrderItem.ITEM2,
  Item3::class to SortOrderItem.ITEM3,
)

private val Any.sortOrderItem get() = orderMapByKClass[this::class]!!

fun main() {
  val expectedOrders = listOf(SortOrderItem.ITEM2, SortOrderItem.ITEM3, SortOrderItem.ITEM1)

  listOf(Item3, Item1(0), Item2("Hello"))
    .sortedBy { expectedOrders.indexOf(it.sortOrderItem) }
    .let { println(it) }
}
