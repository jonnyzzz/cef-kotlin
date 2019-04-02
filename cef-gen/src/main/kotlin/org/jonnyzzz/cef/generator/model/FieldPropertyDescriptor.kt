package org.jonnyzzz.cef.generator.model

import com.squareup.kotlinpoet.TypeName
import org.jonnyzzz.cef.generator.c.StructField
import org.jonnyzzz.cef.generator.c.StructFunctionPointer
import org.jonnyzzz.cef.generator.kn.TypeReplaceableHost
import org.jonnyzzz.cef.generator.kn.fromCefToKotlin

sealed class FieldDescriptor : KDocumented {
  abstract val cFieldName: String

}

data class FieldPropertyDescriptor(
        override val cFieldName: String,
        val propName: String,
        val propType: TypeName,
        private val cefMember: StructField?,
        //the C declared type name, before type mapping
        override val originalTypeName: TypeName? = null
) : FieldDescriptor(), TypeReplaceableHost<FieldPropertyDescriptor> {
  override val type: TypeName
    get() = propType

  override val docComment: String?
    get() = cefMember?.docComment

  override fun replaceType(newType: TypeName) = copy(originalTypeName = propType, propType = newType)
}


data class DetectedFunctionParam(
        val paramName: String,
        val paramType: TypeName,
        //the C declared type name, before type mapping
        override val originalTypeName: TypeName? = null
) : TypeReplaceableHost<DetectedFunctionParam> {
  override val type: TypeName
    get() = paramType

  override fun replaceType(newType: TypeName) = copy(originalTypeName = paramType, paramType = newType)
}


data class FunctionalPropertyDescriptor(
        override val cFieldName: String,
        val funName: String,
        val THIS: DetectedFunctionParam,
        val parameters: List<DetectedFunctionParam>,
        val returnType: TypeName,
        private val cefFunctionPointer: StructFunctionPointer?
) : FieldDescriptor() {

  val parameterNamesList get() = parameters.joinToString(", ") { it.fromCefToKotlin(it.paramName) }

  override val docComment: String?
    get() = cefFunctionPointer?.docComment
}

