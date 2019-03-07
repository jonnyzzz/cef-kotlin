package org.jonnyzzz.cef.generator.c



fun String.asFunArgument(): CFunctionArgument {
  val params = this.split(" ")
  return CFunctionArgument(params.last().trim(), params.dropLast(1).joinToString(" "))
}