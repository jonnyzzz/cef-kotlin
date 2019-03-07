package org.jonnyzzz.cef.generator.kn

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jonnyzzz.cef.generator.GeneratorParameters
import org.jonnyzzz.cef.generator.c.CefStruct
import org.jonnyzzz.cef.generator.cefGeneratedPackage
import org.jonnyzzz.cef.generator.toClassName

class CefKNTypeInfo(
        val rawStruct: ClassName,
        val cefCStruct : CefStruct?
) {
  val cleanName = rawStruct.simpleName.removePrefix("_").removePrefix("cef").removeSuffix("_t")
  val typeName = "Cef" + cleanName.split("_").joinToString("") { it.capitalize() }

  val kInterfaceTypeName = ClassName(cefGeneratedPackage, "K$typeName")
  val kStructTypeName = ClassName(cefGeneratedPackage, "K${typeName}Struct")
  val kImplBaseTypeName = ClassName(cefGeneratedPackage, "K${typeName}ImplBase")

  val pointedName = "pointed_$cleanName"
  val typeClassName = ClassName(cefGeneratedPackage, typeName)
}


fun GeneratorParameters.cefTypeInfo(clazz: ClassDescriptor): CefKNTypeInfo {
  return CefKNTypeInfo(
          clazz.toClassName(),
          cefDeclarations.findStruct(clazz)
  )
}
