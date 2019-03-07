package org.jonnyzzz.cef.generator.kn

import org.jonnyzzz.cef.generator.asNullableCPointer
import org.jonnyzzz.cef.generator.toTypeName


fun CefKNTypeInfo.detectProperties() = sequence<FieldDescriptor>{
  knDescriptor.allMeaningfulProperties().forEach { p ->
    val name = p.name.asString()
    val propName = name.split("_").run {
      first() + drop(1).joinToString("") { it.capitalize() }
    }

    val funType = detectFunctionPropertyType(p)
    if (funType != null) {
      val cefFunction = cefCStruct?.findFunction(p)

      val firstParam = funType.firstOrNull()
      require(firstParam != null && firstParam == rawStruct.asNullableCPointer()) {
        "First parameter of $rawStruct must be self reference, but was $firstParam}"
      }

      val fReturnType = funType.last()
      val THIS = DetectedFunctionParam("THIS", firstParam)
      val fParams = funType.dropLast(1).drop(1).mapIndexed { idx, paramType ->
        DetectedFunctionParam(
                cefFunction?.arguments?.getOrNull(idx + 1)?.name ?: "p$idx",
                paramType
        ).replaceToKotlinTypes()
      }

      yield(FunctionalPropertyDescriptor(name, propName, THIS, fParams, fReturnType, cefFunction))
    } else {
      val cefMember = cefCStruct?.findField(p)
      yield(FieldPropertyDescriptor(name, propName, p.type.toTypeName(), cefMember).replaceToKotlinTypes())
    }
  }
}.toList()

