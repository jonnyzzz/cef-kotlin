package org.jonnyzzz.cef.generator.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jonnyzzz.cef.generator.asNullableCPointer
import org.jonnyzzz.cef.generator.c.CFunctionArgument
import org.jonnyzzz.cef.generator.c.CefStruct
import org.jonnyzzz.cef.generator.c.StructField
import org.jonnyzzz.cef.generator.c.StructFunctionPointer
import org.jonnyzzz.cef.generator.kn.allMeaningfulProperties
import org.jonnyzzz.cef.generator.kn.detectFunctionPropertyType
import org.jonnyzzz.cef.generator.toTypeName


sealed class KStructField : KDocumented {
  abstract val cFieldName: String
}

data class KPropertyField(
        override val cFieldName: String,
        val propName: String,
        val propType: TypeName,
        private val cefMember: StructField?
) : KStructField() {
  override val docComment: String?
    get() = cefMember?.docComment
}


data class DetectedFunctionParam(
        val paramName: String,
        val paramType: TypeName,
        val cefParam: CFunctionArgument?
)

data class KFunctionalField(
        override val cFieldName: String,
        val funName: String,
        val THIS: DetectedFunctionParam,
        val parameters: List<DetectedFunctionParam>,
        val returnType: TypeName,
        private val cefFunctionPointer: StructFunctionPointer?
) : KStructField() {

  override val docComment: String?
    get() = cefFunctionPointer?.docComment
}




fun detectProperties(clazz: ClassDescriptor,
                     cefCStruct: CefStruct?,
                     rawStruct: ClassName): List<KStructField> {
  val members = sequence<KStructField> {
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
        val THIS = DetectedFunctionParam("_this", firstParam, cefFunction?.arguments?.getOrNull(0))
        val fParams = funType.dropLast(1).drop(1).mapIndexed { idx, paramType ->
          //TODO: assert there is no object and obj parameters at the same function

          val cefArgument = cefFunction?.arguments?.getOrNull(idx + 1)
          var paramName = cefArgument?.name ?: "p$idx"

          //TODO: fix related javadoc!
          if (paramName == "object") paramName = "obj"

          DetectedFunctionParam(
                  paramName,
                  paramType,
                  cefArgument
          )
        }

        // Implementation plan
        // - map return types and parameters (requires a list of all types for mapping
        // - map Int to bool (e.g. from javadoc return? true (1) and false (0) messages )

        //TODO: replace |name| with [name] for kDoc
        yield(KFunctionalField(name, propName, THIS, fParams, fReturnType, cefFunction))
      } else {
        val cefMember = cefCStruct?.findField(p)
        yield(KPropertyField(name, propName, p.type.toTypeName(), cefMember))
      }
    }
  }.toList()

  if (cefCStruct == null) return members

  val membersIndex = cefCStruct.structFieldsOrder.mapIndexed { k, v -> v to k }.toMap()
  val originalNames = members.mapIndexed { k, v -> v.cFieldName to (k + membersIndex.size) }.toMap()
  return members.sortedWith(compareBy { membersIndex[it.cFieldName] ?: originalNames[it.cFieldName] ?: 0 })
}

