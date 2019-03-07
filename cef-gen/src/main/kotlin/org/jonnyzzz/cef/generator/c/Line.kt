package org.jonnyzzz.cef.generator.c


data class Line(val no: Int, val text: String) {
  override fun toString() = "${no.toString().padStart(5)}: $text"
}

