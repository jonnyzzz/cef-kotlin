package org.jonnyzzz.cef.generator.c


data class GlobalFunctionNode(
        val function: LeafNode,
        val comment: DocCommentNode
) {
  override fun toString() = ourBuildString {
    appendln(comment)
    appendln(function)
  }

  val docComment = comment.commentText

  val returnType: String
  val functionName: String
  val arguments : List<String>

  init {
    val tokens = function.line.text.trim().split(" ", limit = 3)
    if (tokens.size <= 3) error("Unexpected global function line")
    val (export, type, nameAndParams) = tokens
    if (export != "CEF_EXPORT") error("No CEF_EXPORT found")
    returnType = type

    val nameAndParamsClean = nameAndParams.split(")")
    if (nameAndParamsClean.size != 2) error("Too many ) in the declaration")

    val nameAndParamsInside = nameAndParams.split("(")
    if (nameAndParamsInside.size != 2) error("Too many ( in the declaration")
    functionName = nameAndParamsInside[0]

    arguments = nameAndParamsInside[1].split(",")
  }
}

fun lookupGlobalFunctions(tree: List<BracketsTreeNode>) = sequence {
  val nodes = tree
          .filterNot { it is BlockNode }
          .iterator()

  var prevCommentNode: DocCommentNode? = null

  while (true) {
    val node = nodes.nextOrNull() ?: break

    if (node is LeafNode && node.line.text.trim().startsWith("CEF_EXPORT")) {

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

