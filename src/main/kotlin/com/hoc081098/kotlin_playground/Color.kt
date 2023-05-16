package com.hoc081098.kotlin_playground

enum class Color(val colorName: String, val rgb: String) {
  RED("Red", "#FF0000"),
  ORANGE("Orange", "#FF7F00"),
  YELLOW("Yellow", "#FFFF00")
}

@OptIn(ExperimentalStdlibApi::class)
fun findByRgb(rgb: String): Color? = Color.entries.find { it.rgb == rgb }

@ExperimentalStdlibApi
fun main() {
  println(Color.entries)
  println(Color.entries === Color.entries)
  
  println(findByRgb("#FF0000"))
  println(findByRgb("#FF7F00"))
  println(findByRgb("#FFFF00"))
  println(findByRgb("#FF00FF"))
}