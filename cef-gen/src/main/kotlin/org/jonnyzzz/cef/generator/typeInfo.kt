package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.types.TypeSubstitution


fun CefTypeInfo(clazz: ClassDescriptor) = CefTypeInfo(clazz.toClassName())

data class CefTypeInfo(
        val rawStruct: ClassName
) {
  val cleanName = rawStruct.simpleName.removePrefix("_").removePrefix("cef").removeSuffix("_t")
  val typeName = "Cef" + cleanName.split("_").joinToString("") { it.capitalize() }

  val kInterfaceName = "K$typeName"
  val kInterfaceTypeName = ClassName(cefGeneratedPackage, kInterfaceName)

  val kStructName = "${kInterfaceName}Struct"
  val kStructTypeName = ClassName(cefGeneratedPackage, kStructName)

  val kImplBaseTypeName = ClassName(cefGeneratedPackage, "${kInterfaceName}ImplBase")

  val pointedName = "pointed_$cleanName"

  val typeClassName = ClassName(cefGeneratedPackage, typeName)
}


val ClassDescriptor.isCefBased: Boolean
  get() {
    return getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
            .filter { it.shouldBePrinted }
            .filterIsInstance<PropertyDescriptor>()
            .firstOrNull { it.name.asString() == "base" }?.let {
              it.returnType?.toTypeName() == cefBaseRefCounted
            } ?: false
  }
