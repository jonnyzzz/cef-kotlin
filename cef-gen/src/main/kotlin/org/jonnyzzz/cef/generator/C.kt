package org.jonnyzzz.cef.generator

import org.jonnyzzz.cef.generator.c.Line
import org.jonnyzzz.cef.generator.c.StructField
import org.jonnyzzz.cef.generator.c.StructFunctionPointer
import org.jonnyzzz.cef.generator.c.filterMacros
import org.jonnyzzz.cef.generator.c.lookupGlobalFunctions
import org.jonnyzzz.cef.generator.c.lookupStructs
import org.jonnyzzz.cef.generator.c.parseBlocks
import java.io.File


fun main() {
  val hackIncludes = "/Users/jonnyzzz/Work/cef-kotlin/deps-cef/build/cef_binaries_base/cef_mac/include/capi"
  val testFile = File(hackIncludes, "cef_app_capi.h")
  processCIncludeFile(testFile)
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

  println("=================")
  println()
  println()

  val globalFunctions = lookupGlobalFunctions(blocks)
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

  val structs = lookupStructs(blocks)
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
