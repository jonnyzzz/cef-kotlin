package org.jonnyzzz.cef.generator.c

interface CDocumented {
  val docComment: String
}

interface CFunction : CDocumented {
  val returnType: String
  val functionName: String
  val arguments: List<String>
}

