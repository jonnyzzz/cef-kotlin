package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeProjection

fun GeneratorParameters.generateValFunctions(props: List<PropertyDescriptor>) {
  val poet = FileSpec.builder(
          "org.jonnyzzz.cef.generated",
          "globals"
  ).addImport("kotlinx.cinterop","invoke")

  props.forEach { generateValFunctionsPointer(it, poet) }
  poet.build().writeTo(outputDir)
}

fun detectFunction(prop: PropertyDescriptor, funName: String) : Pair<FunSpec.Builder, List<TypeProjection>>? {
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
          .addAnnotation(ClassName("kotlin", "ExperimentalUnsignedTypes"))

  val fReturnType = funType.type.arguments.last()
  val fParams = funType.type.arguments.dropLast(1)

  fParams.forEachIndexed { idx, it ->
    fSpec.addParameter("p_$idx", it.type.toTypeName())
  }

  fSpec.returns(fReturnType.type.toTypeName())
  return fSpec to fParams
}

private fun generateValFunctionsPointer(prop: PropertyDescriptor, poet: FileSpec.Builder) {
  val (fSpec, fParams) = detectFunction(prop, "safe_" + prop.name) ?: return
  fSpec.addStatement("return (${prop.fqNameSafe.asString()}!!)(${fParams.mapIndexed{idx, _ -> "p_$idx"}.joinToString(", ")})")

  println("${prop.name}  ${prop.returnType?.javaClass}: ${prop.returnType}")
  poet.addFunction(fSpec.build())
}
