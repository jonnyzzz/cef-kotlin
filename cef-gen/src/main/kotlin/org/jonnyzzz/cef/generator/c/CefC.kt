package org.jonnyzzz.cef.generator.c

import java.io.File


class CefFunction(
        val file: CFileInfo,
        val function: GlobalFunctionNode
)

class CefStruct(
        val file: CFileInfo,
        val function: StructNode
)

class CefDeclarations(
        private val files: List<CFileInfo>
) {

  val functions: Map<String, CefFunction> = run {
    files.flatMap { file ->
      file.globalFunctions.map { it.functionName to CefFunction(file, it) }
    }.groupBy { it.first }.mapValues { it.value.single().second }

  }

  val structs: Map<String, CefStruct> = run {
    files.flatMap { file ->
      file.structs.flatMap {
        val s = CefStruct(file, it)
        listOf(it.structTypeName to s, it.typedefTypeName to s)
      }
    }.groupBy { it.first }.mapValues { it.value.single().second }
  }

  override fun toString(): String {
    return "CefDeclarations(files=${files.size}, functions=${functions.size}, types=${structs.size / 2})"
  }
}


fun loadCefDeclarations(allHeaders: List<File>): CefDeclarations {
  val fileInfos = allHeaders.map { header ->
    try {
      parseCFile(header)
    } catch (t: Throwable) {
      throw Error("Failed to parse ${header.name}", t)
    }
  }

  return CefDeclarations(fileInfos)
}

