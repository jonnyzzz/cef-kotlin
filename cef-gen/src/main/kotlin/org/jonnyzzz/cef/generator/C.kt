package org.jonnyzzz.cef.generator

import org.antlr.v4.runtime.RuleContext
import org.jonnyzzz.cef.gen.c.CParser
import java.io.File


fun main() {
  val hackIncludes = "/Users/jonnyzzz/Work/cef-kotlin/deps-cef/build/cef_binaries_base/cef_mac/include/capi"
  val testFile = File(hackIncludes, "cef_app_capi.h")
  processCIncludeFile(testFile)
}


data class Line(val no: Int, val text: String) {
  override fun toString() = "${no.toString().padStart(5)}: $text"
}

sealed class BracketsTreeNode {
  data class LeafNode(val line: Line) : BracketsTreeNode() {
    override fun toString() = "L$line"
  }

  data class BlockNode(val openLine: Line,
                       val closeLine: Line,
                       val children: List<BracketsTreeNode>) : BracketsTreeNode() {
    override fun toString() = buildString {
      appendln("BO$openLine")
      children.forEach {
        appendln("B$it")
      }
      appendln("BC$closeLine")
    }
  }
}

private fun <T : Any> Iterator<T>.nextOrNull() = when (hasNext()) {
  true -> next()
  else -> null
}

private fun parseBlocks(lines: Iterator<Line>) : List<BracketsTreeNode> {
  val result = mutableListOf<BracketsTreeNode>()

  loop@ while(true) {
    val line = lines.nextOrNull() ?: break
    val text = line.text
    when {
      text.isBlank() -> continue@loop

      text.contains("{") -> {
        val subLines = parseBlocks(lines)
        val lastLine = subLines.last() as BracketsTreeNode.LeafNode
        result += BracketsTreeNode.BlockNode(line, lastLine.line, subLines.dropLast(1))
      }

      text.contains("}") -> {
        result += BracketsTreeNode.LeafNode(line)
        break@loop
      }

      else -> result += BracketsTreeNode.LeafNode(line)
    }
  }


  return result
}

private fun filterMacros(lines: Iterator<Line>) = sequence {
  while(true) {
    val line = lines.nextOrNull() ?: break

    if (line.text.trim().startsWith("#include")) continue
    if (line.text.trim().startsWith("#ifndef")) continue
    if (line.text.trim().startsWith("#endif")) continue
    if (line.text.trim().startsWith("#define")) continue
    if (line.text.trim().startsWith("#pragma")) continue

    if (line.text.trim().startsWith("#else")) error("Not supported line $line")
    if (line.text.trim().startsWith("#elsif")) error("Not supported line $line")

    if (line.text.trim().startsWith("#ifdef")) {
      while (true) {
        @Suppress("NAME_SHADOWING")
        val line = lines.nextOrNull() ?: break
        if (line.text.trim().startsWith("#endif")) break
      }
      continue
    }

    yield(line)
  }
}


private fun processCIncludeFile(includeFile: File) {
  val includeFileName = includeFile.nameWithoutExtension

  val fileLines = includeFile.readText()
          .split(Regex("\r?\n"))
          .mapIndexed { id, it -> Line(id, it) }
          .let { filterMacros(it.iterator()) }

  val iterator = fileLines.iterator()
  val blocks = parseBlocks(iterator)
  if (iterator.hasNext()) {
    error("File $includeFileName was not fully parsed ${iterator.asSequence().joinToString("\n")}")
  }

  blocks.forEach {
    println(it)
  }

/*
  val testFileData = includeFile.readText()
          .filterNot {
            it.trim().startsWith("#") ||
                    it.trim().startsWith("extern ")
          }.map {
            it
                    .replace("CEF_CALLBACK", "")
                    .replace("CEF_EXPORT", "")
          }

  val buffer = CodePointBuffer.withChars(CharBuffer.wrap(testFileData.joinToString("\n").toCharArray()))
  val fromBuffer = CodePointCharStream.fromBuffer(buffer)
  val cLexer = CLexer(fromBuffer)
  val tokenStream = CommonTokenStream(cLexer)
  val cParser = CParser(tokenStream)

  val unit = cParser.compilationUnit()

  AstPrinter.print(unit)

  println(unit)*/
}


object AstPrinter {
  fun print(ctx: RuleContext) {
    explore(ctx, 0)
  }

  private fun explore(ctx: RuleContext, indentation: Int) {
    val ruleName = CParser.ruleNames[ctx.ruleIndex]
    for (i in 0 until indentation) {
      print("  ")
    }
    println(ruleName + "    " + ctx.text)
    for (i in 0 until ctx.childCount) {
      val element = ctx.getChild(i)
      if (element is RuleContext) {
        explore(element, indentation + 1)
      }
    }
  }
}