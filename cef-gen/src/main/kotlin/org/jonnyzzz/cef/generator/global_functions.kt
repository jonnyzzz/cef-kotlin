package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jonnyzzz.cef.generator.kn.detectFunctionProperty

fun GeneratorParameters.generateValFunctions(props: List<PropertyDescriptor>) {
  val poet = FileSpec.builder(
          "org.jonnyzzz.cef.generated",
          "globals"
  ).addImport("kotlinx.cinterop", "invoke")
          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build())

  //TODO: include all global functions to provide comments and use typed API
  props.forEach { generateValFunctionsPointer(it, poet) }
  poet.build().writeTo(outputDir)
}

private fun GeneratorParameters.generateValFunctionsPointer(prop: PropertyDescriptor, poet: FileSpec.Builder) {
  val propName = prop.name.asString().split("_").run {
    first() + drop(1).joinToString("") { it.capitalize() }
  }

  val (fSpec, fParams) = detectFunctionProperty(prop, propName) ?: return
  val originalName = prop.fqNameSafe.asString()

  fSpec.addStatement("return ($originalName!!)(${fParams.joinToString(", ") { it.paramName }})")
  fSpec.addAnnotation(ClassName("kotlin", "ExperimentalUnsignedTypes"))

  cefDeclarations.functions[prop.name.asString()]?.function?.docComment?.let {
    fSpec.addKdoc(it)
  }

  println("${prop.name}  ${prop.returnType?.javaClass}: ${prop.returnType}")
  poet.addFunction(fSpec.build())
}
