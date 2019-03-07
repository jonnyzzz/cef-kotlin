package org.jonnyzzz.cef.generator.c


sealed class BracketsTreeNode

data class LeafNode(val line: Line) : BracketsTreeNode() {
  override fun toString() = "L$line"
}

private fun List<Line>.joinToString() = joinToString("") {
  it.text.replace("///", "").replace("//", "").trim()
}.replace(Regex("\\s\\s+"), " ").trim()


data class DocCommentNode(val lines: List<Line>) : BracketsTreeNode() {
  override fun toString() = ourBuildString {
    lines.forEach {
      appendln("D", it)
    }
  }

  val commentText by lazy { lines.joinToString() }
}

data class BlockNode(val openLine: Line,
                     val closeLine: Line,
                     val children: List<BracketsTreeNode>) : BracketsTreeNode() {
  override fun toString() = ourBuildString {
    appendln("BO", openLine)
    children.forEach {
      appendln("B", it)
    }
    appendln("BC", closeLine)
  }
}

data class BracesNode(val lines: List<Line>) : BracketsTreeNode() {
  override fun toString() = ourBuildString {
    lines.forEach {
      appendln("P", it)
    }
  }

  val fullText by lazy { lines.joinToString() }
}
