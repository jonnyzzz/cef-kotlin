package org.jonnyzzz.cef.generator.c


data class GlobalFunctionNode(
        private val function: BracesNode,
        private val comment: DocCommentNode
) : CFunction {
  override fun toString() = ourBuildString {
    appendln(comment)
    appendln(function)
  }

  override val docComment by comment::commentText

  override val returnType: String
  override val functionName: String
  override val arguments : List<String>

  init {
    val fullText = function.fullText
    try {

      fullText.split("(")[0].split(" ").let { returnTypeAndName ->
        if (returnTypeAndName.size < 3) error("Unexpected global function line")
        if (returnTypeAndName.first() != "CEF_EXPORT") error("No CEF_EXPORT found")

        functionName = returnTypeAndName.last().trim()
        returnType = returnTypeAndName.drop(1).dropLast(1).joinToString(" ")
      }

      arguments = fullText.split("(", limit = 2)[1].split(")", limit = 2)[0].split(",")
    } catch (t: Throwable) {
      throw Error("${t.message} in $fullText", t)
    }
  }
}

fun lookupGlobalFunctions(tree: List<BracketsTreeNode>) = sequence {
  val nodes = tree
          .filterNot { it is BlockNode }
          .iterator()

  var prevCommentNode: DocCommentNode? = null

  while (true) {
    val node = nodes.nextOrNull() ?: break

    if (node is BracesNode && node.fullText.trim().startsWith("CEF_EXPORT")) {

      val functionNode = try {
        GlobalFunctionNode(node, prevCommentNode ?: error("no doc-comment block"))
      } catch (t: Throwable) {
        throw Error("Failed to parse global function $node. ${t.message}", t)
      }

      yield(functionNode)
    }

    prevCommentNode = if (node is DocCommentNode) node else null
  }
}.toList()

