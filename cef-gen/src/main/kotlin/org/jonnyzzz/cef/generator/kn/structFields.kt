package org.jonnyzzz.cef.generator.kn

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.types.TypeSubstitution
import org.jonnyzzz.cef.generator.GeneratorParameters
import org.jonnyzzz.cef.generator.shouldBePrinted
import org.jonnyzzz.cef.generator.toTypeName


fun ClassDescriptor.allMeaningfulProperties() =
        getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
                .filter { it.shouldBePrinted }
                .filterIsInstance<PropertyDescriptor>()
                .filter { it.name.asString() !in setOf("size", "base") }


fun ClassDescriptor.allFieldProperties(props: GeneratorParameters, info: CefKNTypeInfo = props.cefTypeInfo(this)) : List<FieldPropertyDescriptor> {
  val cefCStruct = props.cefDeclarations.findStruct(this)

  val allFunctions = allFunctionalProperties(props, info).map { it.cFieldName }.toSet()

  return allMeaningfulProperties().mapNotNull { p ->
    val name = p.name.asString()
    if (name in allFunctions) return@mapNotNull null

    val propName = name.split("_").run {
      first() + drop(1).joinToString("") { it.capitalize() }
    }

    val cefMember = cefCStruct?.findField(p)
    FieldPropertyDescriptor(name, propName, p.type.toTypeName(), cefMember).replaceToKotlinTypes()
  }
}

