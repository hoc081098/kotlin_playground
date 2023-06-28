package com.hoc081098.kotlin_playground

@JvmInline
value class StringWrapper(val value: String)

fun main() {
  val a = StringWrapper("Hello")
  val b = StringWrapper("Hello")
  println(a == b)
  println(a != b)
  
  f1(StringWrapper("Hello")) // boxed
  f2(StringWrapper("Hello")) // not boxed
  f3(StringWrapper("Hello")) // boxed
  
  println("---")
  
  test(Foo(1))
}

fun test(f: Foo) {
  asInline(f)
  asGeneric(f) // boxing
  asInterface(f) // boxing
  asNullable(f) // boxing
  
  val c = id(f) // boxing/unboxing, c is unboxed
  
  foo(ICNullable(null))
  bar(null)
}

fun f1(b: Any?) {}
fun f2(b: StringWrapper?) {
  b?.value
}
fun f3(b: StringWrapper) {
  f2(b)
}

interface I

@JvmInline
value class Foo(val i: Int) : I

fun asInline(f: Foo) {}
fun <T> asGeneric(x: T) {}
fun asInterface(i: I) {}
fun asNullable(i: Foo?) {}
fun <T> id(x: T): T = x

@JvmInline
value class ICNullable(val s: String?)

fun foo(i: ICNullable) {}
fun bar(i: ICNullable?) {}