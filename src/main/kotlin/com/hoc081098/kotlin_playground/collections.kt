package com.hoc081098.kotlin_playground

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.round

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val ints: PersistentList<Int> = (1..100).toPersistentList()
    val index = 40
    println(40.toString(radix = 32))
    val i = ints[index]
    println(i)

    println(14.toString(radix = 2))
    println(MAX_BUFFER_SIZE_MINUS_ONE.toHexString())
    println(MAX_BUFFER_SIZE_MINUS_ONE.inv().toHexString())

    repeat(100) {
        println(it to rootSize(it))
    }
    println("Done")
}

/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */


internal const val MAX_BUFFER_SIZE = 32
internal const val LOG_MAX_BUFFER_SIZE = 5
internal const val MAX_BUFFER_SIZE_MINUS_ONE = MAX_BUFFER_SIZE - 1
internal const val MUTABLE_BUFFER_SIZE = MAX_BUFFER_SIZE + 1

/** Creates new buffer of [MAX_BUFFER_SIZE] capacity having first element initialized with the specified [element]. */
internal fun presizedBufferWith(element: Any?): Array<Any?> {
    val buffer = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
    buffer[0] = element
    return buffer
}

/**
 * Gets trie index segment of the specified [index] at the level specified by [shift].
 *
 * `shift` equal to zero corresponds to the bottommost (leaf) level.
 * For each upper level `shift` increments by [LOG_MAX_BUFFER_SIZE].
 */
internal fun indexSegment(index: Int, shift: Int): Int =
    (index shr shift) and MAX_BUFFER_SIZE_MINUS_ONE

/**
 * Returns the size of trie part of a persistent vector of the specified [vectorSize].
 * find n where n is the multiple of [MAX_BUFFER_SIZE] and n <= [vectorSize] < n + [MAX_BUFFER_SIZE].
 */
internal fun rootSize(vectorSize: Int) =
    (vectorSize - 1) and MAX_BUFFER_SIZE_MINUS_ONE.inv()
