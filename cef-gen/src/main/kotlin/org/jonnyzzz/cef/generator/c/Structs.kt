package org.jonnyzzz.cef.generator.c

data class StructNode(
        private val structBlock: BlockNode,
        private val comment: DocCommentNode
) {
  override fun toString() = ourBuildString {
    appendln(comment)
    appendln(structBlock.openLine.text.trim() + " /*..*/ " + structBlock.closeLine.text.trim())
  }

  val structTypeName = structBlock
          .openLine
          .text
          .split("{")[0]
          .removePrefix("typedef")
          .trim()
          .removePrefix("struct")
          .trim()

  val typedefTypeName = structBlock
          .closeLine
          .text
          .trim()
          .removePrefix("}")
          .trim()
          .removeSuffix(";").trim()

  val docComment = comment.commentText
}

fun lookupStructs(tree: List<BracketsTreeNode>) = sequence {
  val nodes = tree.iterator()

  var prevCommentNode: DocCommentNode? = null

  while (true) {
    val node = nodes.nextOrNull() ?: break

    if (node is BlockNode && node.openLine.text.trim().startsWith("typedef struct")) {

      yield(try {
        StructNode(node, prevCommentNode ?: error("no doc-comment block"))
      } catch (t: Throwable) {
        throw Error("Failed to parse struct typedef $node. ${t.message}", t)
      })
    }

    prevCommentNode = if (node is DocCommentNode) node else null
  }
}.toList()

