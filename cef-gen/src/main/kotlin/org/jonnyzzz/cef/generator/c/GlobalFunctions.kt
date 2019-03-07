package org.jonnyzzz.cef.generator.c


data class GlobalFunctionNode(
        val function: LeafNode,
        val comment: DocCommentNode
) {
  override fun toString() = ourBuildString {
    appendln(comment)
    appendln(function)
  }
}

fun lookupGlobalFunctions(tree: List<BracketsTreeNode>) = sequence {
  val nodes = tree
          .filterNot { it is BlockNode }
          .iterator()

  var prevCommentNode : DocCommentNode? = null

  while(true) {
    val node = nodes.nextOrNull() ?: break

    if (node is LeafNode && node.line.text.trim().startsWith("CEF_EXPORT")) {
      yield(GlobalFunctionNode(node, prevCommentNode ?: error("Global function without comment: $node")))
    }

    prevCommentNode = if (node is DocCommentNode) node else null
  }
}.toList()

