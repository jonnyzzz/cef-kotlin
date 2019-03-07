package org.jonnyzzz.cef.generator.c


fun parseBlocks(lines: Iterator<Line>) : List<BracketsTreeNode> {
  val result = mutableListOf<BracketsTreeNode>()

  loop@ while(true) {
    val line = lines.nextOrNull() ?: break
    val text = line.text
    when {
      text.isBlank() -> continue@loop

      text.contains("{") -> {
        val subLines = parseBlocks(lines)
        val lastLine = subLines.last() as LeafNode
        result += BlockNode(line, lastLine.line, subLines.dropLast(1))
      }

      text.contains("}") -> {
        result += LeafNode(line)
        break@loop
      }

      text.trim().startsWith("///") -> {
        result += readCommentNodes(line, lines)
      }

      text.trim().contains("(") -> {
        result += readBracesNodes(line, lines)
      }

      else -> result += LeafNode(line)
    }
  }

  return result
}

private fun readBracesNodes(firstLine: Line, lines: Iterator<Line>) : BracesNode {
  fun Line.openCount() = text.count { it == '(' }
  fun Line.closeCount() = text.count { it == ')' }

  val result = mutableListOf(firstLine)
  var openCount = firstLine.openCount()
  var closeCount = firstLine.closeCount()

  while(openCount != closeCount) {
    val next = lines.nextOrNull() ?: error("Failed to find ) in the: $result")
    result += next

    openCount += next.openCount()
    closeCount += next.closeCount()
  }

  return BracesNode(result)
}

private fun readCommentNodes(firstLine: Line, lines: Iterator<Line>): DocCommentNode {
  val comments = mutableListOf(firstLine)
  while (true) {
    val line = lines.nextOrNull()
            ?: error("Unexpected end of file inside doc-comment: $firstLine, $comments")

    val text = line.text
    comments += line

    if (text.trim().startsWith("///")) {
      return DocCommentNode(comments)
    }
  }
}

