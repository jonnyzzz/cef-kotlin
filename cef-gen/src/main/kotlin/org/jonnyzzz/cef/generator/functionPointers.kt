package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeSubstitution


data class DetectedFunctionParam(
        val paramName: String,
        val paramType: TypeName,
        //the C declared type name, before type mapping
        val originalTypeName: TypeName? = null
)

fun DetectedFunctionParam.fromCefToKotlin() : String = when {
  originalTypeName == null -> paramName
  originalTypeName == cefString16 -> "$paramName.asString()"
  originalTypeName == cefString16.asNullableCPointer() -> "$paramName?.asString()"
  else -> TODO("Not supported for $this")
}

private fun DetectedFunctionParam.replaceType(newType: TypeName)
        = copy(originalTypeName = this.paramType, paramType = newType)

data class DetectedFunction(
        val funcDeclaration: FunSpec.Builder,
        val params: List<DetectedFunctionParam>,
        val returnType: TypeName
)

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


fun ClassDescriptor.allMeaningfulProperties() =
        getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
                .filter { it.shouldBePrinted }
                .filterIsInstance<PropertyDescriptor>()
                .filter { it.name.asString() !in setOf("size", "base") }


data class FunctionalPropertyDescriptor(
        val cFieldName : String,
        val funName: String,
        val THIS: DetectedFunctionParam,
        val parameters: List<DetectedFunctionParam>,
        val returnType: TypeName
)


fun ClassDescriptor.allFunctionalProperties(info: CefTypeInfo) : List<FunctionalPropertyDescriptor> {
  return allMeaningfulProperties().mapNotNull { p ->
    val name = p.name.asString()
    val propName = name.split("_").run {
      first() + drop(1).joinToString("") { it.capitalize() }
    }

    val funType = detectFunctionPropertyType(p) ?: return@mapNotNull null

    val firstParam = funType.firstOrNull()
    require(firstParam != null && firstParam == info.rawStruct.asNullableCPointer()) {
      "First parameter of ${info.rawStruct} must be self reference, but was $firstParam}"
    }

    val fReturnType = funType.last()
    val THIS = DetectedFunctionParam("THIS", firstParam)
    val fParams = funType.dropLast(1).drop(1).mapIndexed { idx, paramType ->
      val baseParam = DetectedFunctionParam("p$idx", paramType)
      when (paramType) {
        cefString16 -> baseParam.replaceType(kotlinString)
        cefString16.asNullableCPointer() -> baseParam.replaceType(kotlinString.copy(nullable = true))
        else -> baseParam
      }
    }

    FunctionalPropertyDescriptor(name, propName, THIS,fParams, fReturnType )
  }
}