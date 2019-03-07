package org.jonnyzzz.cef.generator.kn

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.SimpleType
import org.jonnyzzz.cef.generator.GeneratorParameters
import org.jonnyzzz.cef.generator.asNullableCPointer
import org.jonnyzzz.cef.generator.toTypeName


fun detectFunctionPropertyType(prop: PropertyDescriptor): List<TypeName>? {
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

 return cFunctionType.type.arguments[0].type.arguments.map { it.toTypeName() }
}

fun detectFunctionProperty(prop: PropertyDescriptor, funName: String): DetectedFunction? {
  val funType = detectFunctionPropertyType(prop) ?: return null
  val fSpec = FunSpec.builder(funName)

  val fReturnType = funType.last()
  val fParams = funType.dropLast(1).mapIndexed { idx, it -> DetectedFunctionParam("p$idx", it) }

  fParams.forEach {
    fSpec.addParameter(it.paramName, it.paramType)
  }

  fSpec.returns(fReturnType)
  return DetectedFunction(fSpec, fParams, fReturnType)
}


