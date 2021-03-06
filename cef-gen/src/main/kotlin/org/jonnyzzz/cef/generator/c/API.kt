package org.jonnyzzz.cef.generator.c

interface CDocumented {
  val docComment: String
}

interface CNamed {
  val name : String
}

interface CFunction : CDocumented, CNamed {
  val returnType: String
  val arguments: List<CFunctionArgument>
}

data class CFunctionArgument(
        override val name: String,
        val type: String
) : CNamed
