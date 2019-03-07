package org.jonnyzzz.cef.generator.c

import java.io.File

data class CFileInfo(
        val file: File,
        val globalFunctions: List<GlobalFunctionNode>,
        val structs: List<StructNode>
)


fun parseCFile(includeFile: File,
               debug: Boolean = false
): CFileInfo {
  val includeFileName = includeFile.nameWithoutExtension

  val fileLines = includeFile.readText()
          .split(Regex("\r?\n"))
          .mapIndexed { id, it -> Line(id, it) }
          .let { filterMacros(it.iterator()) }

  val iterator = fileLines.iterator().asPushBack()
  val blocks = parseBlocks(iterator)
  if (iterator.hasNext()) {
    error("File $includeFileName was not fully parsed ${iterator.asSequence().joinToString("\n")}")
  }

  if (debug) debugParsing(blocks)

  val globalFunctions = lookupGlobalFunctions(blocks)

  if (debug) debugFunctions(globalFunctions)

  val structs = lookupStructs(blocks)
  if (debug) debugStructs(structs)

  return CFileInfo(includeFile, globalFunctions, structs)
}

private fun debugParsing(blocks: List<BracketsTreeNode>) {
  blocks.forEach {
    println(it)
  }

  println("=================")
  println()
  println()
}

private fun debugFunctions(globalFunctions: List<GlobalFunctionNode>) {
  for (globalFunction in globalFunctions) {
    println()
    println(globalFunction)
    println()
    println("//${globalFunction.docComment}")
    println(globalFunction.returnType +" " + globalFunction.functionName + " " + globalFunction.arguments)
    println()
  }
  println("=================")
  println()
  println()
}

private fun debugStructs(structs: List<StructNode>) {
  for (struct in structs) {
    println()
    println(struct)
    println()
    println("//${struct.docComment}")
    println(struct.structTypeName)
    println(struct.typedefTypeName)
    println()
    for (member in struct.members) {
      println()
      println(member)
      println()
      when(member) {
        is StructField -> {
          println("//${member.docComment}")
          println("${member.type} ${member.name}")
        }
        is StructFunctionPointer -> {
          println("//${member.docComment}")
          println("${member.returnType} ${member.functionName} ${member.arguments}")
        }
      }
    }
  }

}