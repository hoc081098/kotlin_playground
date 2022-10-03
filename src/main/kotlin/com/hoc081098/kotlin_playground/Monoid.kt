@file:OptIn(ExperimentalStdlibApi::class)

package com.hoc081098.kotlin_playground

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * A monoid is a set with an associative binary operation ([combine]) and an identity element ([empty]).
 *
 * The binary operation must satisfy the monoid laws:
 * 1. Associativity: `combine(combine(a, b), c) == combine(a, combine(b, c))`
 * 2. Left identity: `combine(empty, a) == a`
 * 2. Left identity: `combine(a, empty) == a`
 */
interface Monoid<T> {
  val empty: T
  fun combine(a: T, b: T): T
}

context(Monoid<T>)
fun <T> T.combine(other: T): T = combine(this, other)

context(Monoid<T>)
fun <T> Iterable<T>.fold(): T = fold(empty) { acc, e -> acc.combine(e) }

context(Monoid<R>)
fun <T, R> Iterable<T>.foldMap(transform: (T) -> R): R = fold(empty) { acc, e -> combine(acc, transform(e)) }

context(Monoid<R>) suspend fun <T, R> Iterable<T>.parFoldMap(
  f: suspend (T) -> R,
  maxConcurrent: Int,
): R = coroutineScope {
  val semaphore = Semaphore(maxConcurrent)

  map { item ->
    async {
      semaphore.withPermit {
        f(item)
      }
    }
  }.awaitAll().fold()
}

fun <A> listMonoid(): Monoid<List<A>> = object : Monoid<List<A>> {
  override val empty: List<A> get() = emptyList()
  override fun combine(a: List<A>, b: List<A>): List<A> = a + b
}

@JvmField
val stringMonoid = object : Monoid<String> {
  override val empty: String get() = ""
  override fun combine(a: String, b: String): String = a + b
}

@JvmField
val intMonoid = object : Monoid<Int> {
  override val empty: Int get() = 0
  override fun combine(a: Int, b: Int): Int = a + b
}

fun main() {
  val sum = intMonoid.run { listOf(1, 2, 3, 4, 5).fold() }
  println(sum)

  val concatenated = stringMonoid.run { listOf(1, 2, 3, 4, 5).foldMap(Int::toString) }
  println(concatenated)

  runBlocking {
    val pages = 0..<10
    val users = listMonoid<User>().run { pages.parFoldMap(::getUsers, 3) }
    println(users)
  }
}

suspend fun getUsers(page: Int, perPage: Int = 5): List<User> {
  println("getUsers($page)")
  delay(2_000)
  return (page * perPage ..< (page + 1) * perPage).map { User(it, "User $it") }
}

data class User(val id: Int, val name: String)

// --------------------------------------------------

typealias Req = String
typealias Item = String

fun demo() {
  fun <A, B> function1Monoid(bMonoid: Monoid<B>): Monoid<(A) -> B> = object : Monoid<(A) -> B> {
    override val empty: (A) -> B get() = { bMonoid.empty }
    override fun combine(a: (A) -> B, b: (A) -> B): (A) -> B = { bMonoid.combine(a(it), b(it)) }
  }

  fun userItems(req: Req): List<Item> = emptyList()
  fun postItems(req: Req): List<Item> = emptyList()
  fun feedItems(req: Req): List<Item> = emptyList()

  function1Monoid<Req, List<Item>>(listMonoid()).run {
    listOf(::userItems, ::postItems, ::feedItems).fold()
  }
}