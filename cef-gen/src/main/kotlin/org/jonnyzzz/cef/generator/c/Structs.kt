package org.jonnyzzz.cef.generator.c

sealed class StructMember

data class StructField(
        private val line: LeafNode,
        private val comment: DocCommentNode
) : StructMember() {
  val docComment = comment.commentText

  override fun toString() = ourBuildString {
    appendln(comment)
    appendln(line)
  }
}

data class StructFunctionPointer(
        private val block: BracesNode,
        private val comment: DocCommentNode
) : StructMember() {
  val docComment = comment.commentText

  override fun toString() = ourBuildString {
    appendln(comment)
    appendln(block)
  }
}

data class StructNode(
        private val structBlock: BlockNode,
        private val comment: DocCommentNode,
        val members: List<StructMember>
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
        StructNode(
                node,
                prevCommentNode ?: error("no doc-comment block"),
                parseStructMembers(node.children)
        )
      } catch (t: Throwable) {
        throw Error("Failed to parse struct typedef $node. ${t.message}", t)
      })
    }

    prevCommentNode = if (node is DocCommentNode) node else null
  }
}.toList()


private fun parseStructMembers(blockNodes: List<BracketsTreeNode>) = sequence {
  val nodes = blockNodes.iterator()

  var prevCommentNode: DocCommentNode? = null

  while (true) {
    val node = nodes.nextOrNull() ?: break

    if (node is BracesNode) {
      yield(try {
        StructFunctionPointer(node, prevCommentNode ?: error("no doc-comment block"))
      } catch (t: Throwable) {
        throw Error("Failed to parse struct typedef $node. ${t.message}", t)
      })
    }

    if (node is LeafNode) {
      yield(try {
        StructField(node, prevCommentNode ?: error("no doc-comment block"))
      } catch (t: Throwable) {
        throw Error("Failed to parse struct typedef $node. ${t.message}", t)
      })
    }

    prevCommentNode = if (node is DocCommentNode) node else null
  }
}.toList()
