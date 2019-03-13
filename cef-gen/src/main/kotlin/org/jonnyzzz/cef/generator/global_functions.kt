package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jonnyzzz.cef.generator.kn.DetectedFunctionParam
import org.jonnyzzz.cef.generator.kn.detectFunctionPropertyType

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

  val funType = detectFunctionPropertyType(prop) ?: return

  val cefFunction = cefDeclarations.findFunction(prop)

  val fSpec = FunSpec.builder(propName)

  val fReturnType = funType.last()
  val fParams = funType.dropLast(1).mapIndexed { idx, it ->
    DetectedFunctionParam(cefFunction?.function?.arguments?.getOrNull(idx)?.name ?: "p$idx", it)
  }

  fParams.forEach {
    fSpec.addParameter(it.paramName, it.paramType)
  }

  fSpec.returns(fReturnType)

  val originalName = prop.fqNameSafe.asString()

  fSpec.addStatement("return ($originalName!!)(${fParams.joinToString(", ") { it.paramName }})")

  cefFunction?.function?.docComment?.let {
    fSpec.addKdoc(it)
  }

  poet.addFunction(fSpec.build())
}
