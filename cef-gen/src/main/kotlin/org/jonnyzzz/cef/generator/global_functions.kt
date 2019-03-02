package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.SimpleType

fun GeneratorParameters.generateValFunctions(props: List<PropertyDescriptor>) {
  val poet = FileSpec.builder(
          "org.jonnyzzz.cef.generated",
          "globals"
  ).addImport("kotlinx.cinterop", "invoke")
          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build())

  props.forEach { generateValFunctionsPointer(it, poet) }
  poet.build().writeTo(outputDir)
}

fun detectFunction(prop: PropertyDescriptor, funName: String) : Pair<FunSpec.Builder, List<Pair<String, TypeName>>>? {
  val returnType = prop.returnType as? SimpleType
  if (returnType == null) {
    println("PROP: ${prop.name}  ${prop.returnType?.javaClass}: ${prop.returnType}  - Unknown Return Type")
    return null
  }

  if (returnType.constructor.declarationDescriptor.classId != ClassId.fromString("kotlinx/cinterop/CPointer")) {
    println("PROP: ${prop.name}  ${prop.returnType?.javaClass}: ${prop.returnType}  - Not CPointer<*>")
    return null
  }

  val cFunctionType = returnType.arguments.getOrNull(0)
  if (cFunctionType?.type?.constructor?.declarationDescriptor?.classId != ClassId.fromString("kotlinx/cinterop/CFunction")) {
    println("PROP: ${prop.name}  ${prop.returnType?.javaClass}: ${prop.returnType}  - Not CPointer<CFunction<*>>")
    return null
  }

  val funType = cFunctionType.type.arguments[0]

  val fSpec = FunSpec.builder(funName)

  val fReturnType = funType.type.arguments.last()
  val fParams = funType.type.arguments.dropLast(1).mapIndexed { idx, it -> "p_$idx" to it.type.toTypeName() }

  fParams.forEach{ (idx, it) ->
    fSpec.addParameter(idx, it)
  }

  fSpec.returns(fReturnType.type.toTypeName())
  return fSpec to fParams
}

private fun generateValFunctionsPointer(prop: PropertyDescriptor, poet: FileSpec.Builder) {
  val (fSpec, fParams) = detectFunction(prop, "safe_" + prop.name) ?: return
  fSpec.addStatement("return (${prop.fqNameSafe.asString()}!!)(${fParams.joinToString(", ") { (idx, _) -> idx }})")
  fSpec.addAnnotation(ClassName("kotlin", "ExperimentalUnsignedTypes"))

  println("${prop.name}  ${prop.returnType?.javaClass}: ${prop.returnType}")
  poet.addFunction(fSpec.build())
}
