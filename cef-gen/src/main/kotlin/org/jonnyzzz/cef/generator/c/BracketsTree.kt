package org.jonnyzzz.cef.generator.c


sealed class BracketsTreeNode

data class LeafNode(val line: Line) : BracketsTreeNode() {
  override fun toString() = "L$line"
}

data class DocCommentNode(val lines: List<Line>) : BracketsTreeNode() {
  override fun toString() = ourBuildString {
    lines.forEach {
      appendln("D", it)
    }
  }
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

