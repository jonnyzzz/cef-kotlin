package org.jonnyzzz.cef.generator.kn

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jonnyzzz.cef.generator.GeneratorParameters
import org.jonnyzzz.cef.generator.cefGeneratedPackage
import org.jonnyzzz.cef.generator.toClassName

interface CefKNTypeInfo {
  val rawStruct: ClassName
  val cleanName get() = rawStruct.simpleName.removePrefix("_").removePrefix("cef").removeSuffix("_t")
  val typeName get() = "Cef" + cleanName.split("_").joinToString("") { it.capitalize() }

  val kInterfaceTypeName get() = ClassName(cefGeneratedPackage, "K$typeName")
  val kStructTypeName get() = ClassName(cefGeneratedPackage, "K${typeName}Struct")
  val kImplBaseTypeName get() = ClassName(cefGeneratedPackage, "K${typeName}ImplBase")

  val pointedName get() = "pointed_$cleanName"
  val typeClassName get() = ClassName(cefGeneratedPackage, typeName)
}


fun GeneratorParameters.cefTypeInfo(clazz: ClassDescriptor) = object : CefKNTypeInfo {
  override val rawStruct: ClassName
    get() = clazz.toClassName()
}
