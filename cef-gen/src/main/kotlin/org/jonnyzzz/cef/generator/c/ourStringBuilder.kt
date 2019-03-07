package org.jonnyzzz.cef.generator.c


interface OurStringBuilder {
  fun appendln(x: Any)
  fun appendln(suffix: String, x: Any)
}

fun ourBuildString(builder: OurStringBuilder.() -> Unit) : String {
  val strings = mutableListOf<String>()
  object:OurStringBuilder {
    override fun appendln(suffix: String, x: Any) {
      x.toString().split("\n").mapTo(strings) { "$suffix$it" }
    }

    override fun appendln(x: Any) {
      strings += x.toString()
    }

  }.builder()
  return strings.joinToString("\n")
}

