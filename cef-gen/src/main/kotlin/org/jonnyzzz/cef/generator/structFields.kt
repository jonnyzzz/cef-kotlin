package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.types.TypeSubstitution


interface CefPropertyName {
  val cFieldName : String
}

fun ClassDescriptor.allMeaningfulProperties() =
        getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
                .filter { it.shouldBePrinted }
                .filterIsInstance<PropertyDescriptor>()
                .filter { it.name.asString() !in setOf("size", "base") }


data class FieldPropertyDescriptor(
        override val cFieldName : String,
        val propName: String,
        val propType: TypeName,
        //the C declared type name, before type mapping
        override val originalTypeName: TypeName? = null,
        val visibleInInterface : Boolean = true
) : TypeReplaceableHost<FieldPropertyDescriptor>, CefPropertyName {
  override val type: TypeName
    get() = propType

  override fun replaceType(newType: TypeName) = copy(originalTypeName = propType, propType = newType)
}

fun ClassDescriptor.allFieldProperties(props: GeneratorParameters, info: CefTypeInfo = CefTypeInfo(this)) : List<FieldPropertyDescriptor> {
  val allFunctions = allFunctionalProperties(props, info).map { it.cFieldName }.toSet()

  return allMeaningfulProperties().mapNotNull { p ->
    val name = p.name.asString()
    if (name in allFunctions) return@mapNotNull null

    val propName = name.split("_").run {
      first() + drop(1).joinToString("") { it.capitalize() }
    }

    FieldPropertyDescriptor(name, propName, p.type.toTypeName()).replaceToKotlinTypes()
  }
}

