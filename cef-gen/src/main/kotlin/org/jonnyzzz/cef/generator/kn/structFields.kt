package org.jonnyzzz.cef.generator.kn

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jonnyzzz.cef.generator.asNullableCPointer
import org.jonnyzzz.cef.generator.c.CefStruct
import org.jonnyzzz.cef.generator.toTypeName


fun detectProperties(clazz: ClassDescriptor,
                     cefCStruct: CefStruct?,
                     rawStruct: ClassName): List<FieldDescriptor> {
  val members = sequence<FieldDescriptor> {
    clazz.allMeaningfulProperties().forEach { p ->
      val name = p.name.asString()
      val propName = name.tokenizeNames().run {
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
        val THIS = DetectedFunctionParam("_this", firstParam)
        val fParams = funType.dropLast(1).drop(1).mapIndexed { idx, paramType ->
          //TODO: assert there is no object and obj parameters at the same function

          var paramName = cefFunction?.arguments?.getOrNull(idx + 1)?.name ?: "p$idx"

          //TODO: fix related javadoc!
          if (paramName == "object") paramName = "obj"

          DetectedFunctionParam(
                  paramName,
                  paramType
          ).replaceToKotlinTypes()
        }

        // Implementation plan
        // - map return types and parameters (requires a list of all types for mapping
        // - map Int to bool (e.g. from javadoc return? true (1) and false (0) messages )

        //TODO: replace |name| with [name] for kDoc
        yield(FunctionalPropertyDescriptor(name, propName, THIS, fParams, fReturnType, cefFunction))
      } else {
        val cefMember = cefCStruct?.findField(p)
        yield(FieldPropertyDescriptor(name, propName, p.type.toTypeName(), cefMember, p.isVar).replaceToKotlinTypes())
      }
    }
  }.toList()

  if (cefCStruct == null) return members

  val membersIndex = cefCStruct.structFieldsOrder.mapIndexed { k, v -> v to k }.toMap()
  val originalNames = members.mapIndexed { k, v -> v.cFieldName to (k + membersIndex.size) }.toMap()
  return members.sortedWith(compareBy { membersIndex[it.cFieldName] ?: originalNames[it.cFieldName] ?: 0 })
}

