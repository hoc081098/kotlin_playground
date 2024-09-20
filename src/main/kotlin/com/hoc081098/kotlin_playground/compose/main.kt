package com.hoc081098.kotlin_playground.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.singleWindowApplication

fun main() {
  singleWindowApplication {
    MyComposable()
  }
}

fun func1() {
  println("Func 1")
}

fun func2(i: Int) {
  println("Func 2: $i")
}

@Composable
fun MyComposable() {
  val func3 = {
    println("Func 3")
  }
  val func4 = { i: Int ->
    println("Func 4: $i")
  }

  Column {
    Button(onClick = ::func1) {
      Text("Func 1")
    }
    Button(onClick = { func2(1) }) {
      Text("Func 2")
    }
    Button(onClick = func3) {
      Text("Func 3")
    }
    Button(onClick = { func4(1) }) {
      Text("Func 4")
    }
  }
}
