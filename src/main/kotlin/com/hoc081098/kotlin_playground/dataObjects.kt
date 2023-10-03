package com.hoc081098.kotlin_playground

object NormalObject

data object DataObject

fun main() {
    // operator == of data object compares two object by type, not reference.
    // that means: `other is DataObject`.
    // normal object compares by reference, that means: `other === NormalObject`.

    val newDataObject: DataObject = DataObject::class.java
        .getDeclaredConstructor()
        .apply { isAccessible = true }
        .newInstance()
    println("data object: === should be false -> ${newDataObject === DataObject}")
    println("data object: == should be true -> ${newDataObject == DataObject}")

    println("-".repeat(80))

    val newNormalObject = NormalObject::class.java
        .getDeclaredConstructor()
        .apply { isAccessible = true }
        .newInstance()
    println("normal object: === should be false -> ${newNormalObject === NormalObject}")
    println("normal object: == should be false -> ${newNormalObject == NormalObject}")
}