package org.jonnyzzz.cef.generator.kn

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jonnyzzz.cef.generator.GeneratorParameters
import org.jonnyzzz.cef.generator.c.CefStruct
import org.jonnyzzz.cef.generator.cefGeneratedPackage
import org.jonnyzzz.cef.generator.toClassName

data class CefKNTypeInfo(
        val knDescriptor: ClassDescriptor,
        val cefCStruct : CefStruct?
) : KDocumented {

  override val docComment: String?
    get() = cefCStruct?.docComment

  val rawStruct = knDescriptor.toClassName()

  val cleanName = rawStruct.simpleName.removePrefix("_").removePrefix("cef").removeSuffix("_t")
  val typeName = "Cef" + cleanName.split("_").joinToString("") { it.capitalize() }

  val sourceInterfaceFileName = "K$typeName"
  val sourceKtoCefFileName = "K${typeName}Bridge"

  val kInterfaceTypeName = ClassName(cefGeneratedPackage, "K$typeName")
  val kStructTypeName = ClassName(cefGeneratedPackage, "K${typeName}Struct")
  val kImplBaseTypeName = ClassName(cefGeneratedPackage, "K${typeName}ImplBase")

  val pointedName = "pointed_$cleanName"
  val typeClassName = ClassName(cefGeneratedPackage, typeName)

  val isCefBased by lazy { knDescriptor.isCefBased }

  private val properties by lazy { this.detectProperties() }

  val functionProperties get() = properties.filterIsInstance<FunctionalPropertyDescriptor>()
  val fieldProperties get() = properties.filterIsInstance<FieldPropertyDescriptor>()
}

fun GeneratorParameters.cefTypeInfo(clazz: ClassDescriptor): CefKNTypeInfo {
  return CefKNTypeInfo(
          clazz,
          cefDeclarations.findStruct(clazz)
  )
}
