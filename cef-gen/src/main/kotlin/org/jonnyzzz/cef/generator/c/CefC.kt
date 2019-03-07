package org.jonnyzzz.cef.generator.c

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jonnyzzz.cef.generator.div
import java.io.File
import java.nio.file.Files
import kotlin.streams.toList


class CefFunction(
        val file: CFileInfo,
        val function: GlobalFunctionNode
) : CDocumented {
  override val docComment by function::docComment
}

class CefStruct(
        val file: CFileInfo,
        val struct: StructNode
) : CDocumented {
  override val docComment by struct::docComment

  fun findField(p: PropertyDescriptor) = struct.members[p.name.asString()] as? StructField
  fun findFunction(p: PropertyDescriptor) = struct.members[p.name.asString()] as? StructFunctionPointer
}

class CefDeclarations(
        private val files: List<CFileInfo>
) {

  val functions: Map<String, CefFunction> = run {
    files.flatMap { file ->
      file.globalFunctions.map { it.name to CefFunction(file, it) }
    }.groupBy { it.first }.mapValues { it.value.single().second }
  }.toSortedMap()

  val structs: Map<String, CefStruct> = run {
    files.flatMap { file ->
      file.structs.flatMap {
        val s = CefStruct(file, it)
        listOf(it.structTypeName to s, it.typedefTypeName to s)
      }
    }.groupBy { it.first }.mapValues { it.value.single().second }
  }.toSortedMap()


  fun findStruct(clazz: ClassDescriptor) = structs[clazz.name.asString()]


  override fun toString(): String {
    return "CefDeclarations(files=${files.size}, functions=${functions.size}, types=${structs.size / 2})"
  }
}

fun loadCefDeclarations(includesDir: File): CefDeclarations {
  val allHeaders = Files.walk((includesDir / "capi").toPath())
          .map { it.toFile() }
          .filter { it.isFile && it.name.endsWith(".h") }
          .toList() +
          includesDir / "internal/cef_types.h" +
          includesDir / "internal/cef_types_mac.h" +
          includesDir / "internal/cef_string_types.h"

  val fileInfos = allHeaders.map { header ->
    try {
      parseCFile(header)
    } catch (t: Throwable) {
      throw Error("Failed to parse ${header.name}", t)
    }
  }

  return CefDeclarations(fileInfos)
}

