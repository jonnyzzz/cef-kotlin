package org.jonnyzzz.cef.generator.c

sealed class StructMember : CNamed, CDocumented

data class StructField(
        private val line: LeafNode,
        private val comment: DocCommentNode
) : StructMember(), CDocumented {
  override val docComment by comment::commentText

  override fun toString() = ourBuildString {
    appendln(comment)
    appendln(line)
  }

  val type: String
  override val name: String

  init {
    val text = line.line.text.trim().split(" ").map { it.trim() }
    type = text.dropLast(1).joinToString(" ") { it.trim() }
    name = text.last().trim().removeSuffix(";").trim()
  }
}

data class StructFunctionPointer(
        private val block: BracesNode,
        private val comment: DocCommentNode
) : StructMember(), CFunction {
  override val docComment by comment::commentText

  override fun toString() = ourBuildString {
    appendln(comment)
    appendln(block)
  }

  override val returnType: String
  override val name: String
  override val arguments: List<CFunctionArgument>

  init {
    val fullText = block.fullText.trim().replace(Regex("\\)\\s+\\("), ")(")
    try {
      if (!fullText.contains("CEF_CALLBACK*")) error("Only CEF_CALLBACK function pointers are allowed")

      returnType = fullText.split("(",limit = 2)[0].split(" ").joinToString(" ") { it.trim() }
      this.name = fullText.split(")", limit = 2)[0].split("CEF_CALLBACK*", limit = 2)[1].trim()
      arguments = fullText.split(")(", limit = 2)[1].split(")")[0].split(",").map { it.asFunArgument() }
    } catch (t: Throwable) {
      throw Error("${t.message} in text at ${block.lines.first().no}:\n$fullText", t)
    }
  }
}

data class StructNode(
        private val structBlock: BlockNode,
        private val comment: DocCommentNode,
        val members: Map<String, StructMember>
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
        throw Error("Failed to parse struct typedef in\n$node", t)
      })
    }

    prevCommentNode = if (node is DocCommentNode) node else null
  }
}.toList()


private fun parseStructMembers(blockNodes: List<BracketsTreeNode>) = sequence<StructMember> {
  val nodes = blockNodes.iterator()

  var prevCommentNode: DocCommentNode? = null

  while (true) {
    val node = nodes.nextOrNull() ?: break

    if (node is BracesNode) {
      yield(try {
        StructFunctionPointer(node, prevCommentNode ?: error("no doc-comment block"))
      } catch (t: Throwable) {
        throw Error("Failed to parse struct function pointer in\n$node$", t)
      })
    }

    if (node is LeafNode) {
      yield(try {
        StructField(node, prevCommentNode ?: DocCommentNode(listOf()))
      } catch (t: Throwable) {
        throw Error("Failed to parse struct field in\n$node", t)
      })
    }

    prevCommentNode = if (node is DocCommentNode) node else null
  }
}.groupBy { it.name }.mapValues { it.value.single() }.toSortedMap()
