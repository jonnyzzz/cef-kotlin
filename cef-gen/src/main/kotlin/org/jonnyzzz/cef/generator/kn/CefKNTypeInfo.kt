package org.jonnyzzz.cef.generator.kn

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.types.TypeSubstitution
import org.jonnyzzz.cef.generator.GeneratorParameters
import org.jonnyzzz.cef.generator.c.CefStruct
import org.jonnyzzz.cef.generator.cefBaseRefCounted
import org.jonnyzzz.cef.generator.cefGeneratedPackage
import org.jonnyzzz.cef.generator.shouldBePrinted
import org.jonnyzzz.cef.generator.toClassName
import org.jonnyzzz.cef.generator.toTypeName

data class CefKNTypeInfo(
        val rawStruct: ClassName,
        private val properties: List<FieldDescriptor>,

        val cefCStruct : CefStruct?,
        val cefBased : CefKNTypeInfo?
) : KDocumented {

  override val docComment: String?
    get() = cefCStruct?.docComment

  val cleanName = rawStruct.simpleName.removePrefix("_").removePrefix("cef").removeSuffix("_t")
  val typeName = "Cef" + cleanName.split("_").joinToString("") { it.capitalize() }

  val sourceInterfaceFileName = "K$typeName"
  val sourceKtoCefFileName = "K${typeName}Bridge"

  val kInterfaceTypeName = ClassName(cefGeneratedPackage, "K$typeName")
  val kStructTypeName = ClassName(cefGeneratedPackage, "K${typeName}Struct")
  val kImplBaseTypeName = ClassName(cefGeneratedPackage, "K${typeName}ImplBase")

  val pointedName = "pointed_$cleanName"
  val typeClassName = ClassName(cefGeneratedPackage, typeName)

  val isCefBased get() = cefBased != null


  val functionProperties get() = properties.filterIsInstance<FunctionalPropertyDescriptor>()
  val fieldProperties get() = properties.filterIsInstance<FieldPropertyDescriptor>()
}

fun GeneratorParameters.cefTypeInfo(clazz: ClassDescriptor): CefKNTypeInfo {

  val isCefBased = clazz.getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
          .filter { it.shouldBePrinted }
          .filterIsInstance<PropertyDescriptor>()
          .firstOrNull { it.name.asString() == "base" }?.let {
            it.returnType?.toTypeName() == cefBaseRefCounted
          } ?: false

  val rawStruct = clazz.toClassName()
  val cefStruct = cefDeclarations.findStruct(clazz)

  val properties = detectProperties(clazz, cefStruct, rawStruct)

  return CefKNTypeInfo(
          rawStruct,
          properties,
          cefStruct,
          if(isCefBased) cefBaseClassDescriptorInfo else null
  )
}
