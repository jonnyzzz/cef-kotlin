package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

fun GeneratorParameters.generateValFunctions(props: List<PropertyDescriptor>) {
  val poet = FileSpec.builder(
          "org.jonnyzzz.cef.generated",
          "globals"
  ).addImport("kotlinx.cinterop", "invoke")
          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build())

  props.forEach { generateValFunctionsPointer(it, poet) }
  poet.build().writeTo(outputDir)
}

private fun generateValFunctionsPointer(prop: PropertyDescriptor, poet: FileSpec.Builder) {
  val (fSpec, fParams) = detectFunctionProperty(prop, "safe_" + prop.name) ?: return
  fSpec.addStatement("return (${prop.fqNameSafe.asString()}!!)(${fParams.joinToString(", ") { it.paramName }})")
  fSpec.addAnnotation(ClassName("kotlin", "ExperimentalUnsignedTypes"))

  println("${prop.name}  ${prop.returnType?.javaClass}: ${prop.returnType}")
  poet.addFunction(fSpec.build())
}
