package com.hoc081098.kotlin_playground

@JvmInline
value class Person(private val fullName: String) {
  // Allowed since Kotlin 1.4.30:
  init {
    check(fullName.isNotBlank()) {
      "Full name shouldn't be empty"
    }
  }
  
  // Preview available since Kotlin 1.8.20:
  constructor(name: String, lastName: String) : this("$name $lastName") {
    check(lastName.isNotBlank()) {
      "Last name shouldn't be empty"
    }
  }
}

fun main() {
  println(Person("Hoc081098"))
  println(Person("Hoc081098", "Nguyen"))
  println(Person("Hoc081098", ""))
}